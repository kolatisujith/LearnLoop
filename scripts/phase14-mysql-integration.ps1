# Run LearnLoop's opt-in JDBC integration tests against an isolated disposable MySQL instance.
# This script never connects to the developer's MySQL80 instance on port 3306.
$ErrorActionPreference = 'Stop'
$root = 'C:\Users\prath\eclipse-workspace\LearnLoop'
$work = 'C:\Users\prath\eclipse-workspace\_LearnLoop_Phase14MySQLTestData'
$data = Join-Path $work 'data'
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$mysqld = Join-Path $mysqlBin 'mysqld.exe'
$mysql = Join-Path $mysqlBin 'mysql.exe'
$mysqlAdmin = Join-Path $mysqlBin 'mysqladmin.exe'
$port = 3307
$report = Join-Path $root 'docs\phase14-integration-result.md'
$log = Join-Path $root 'docs\phase14-integration-build.log'
$server = $null
$buildExit = 1
$serverStarted = $false

if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
    throw "Port $port is already in use. Refusing to start or stop an unknown service."
}
if (!(Test-Path $mysqld) -or !(Test-Path $mysql)) {
    throw 'MySQL Server 8.0 binaries were not found at the expected installation path.'
}
if (!(Test-Path $data)) {
    New-Item -ItemType Directory -Path $data -Force | Out-Null
}
if (!(Test-Path (Join-Path $data 'mysql.ibd'))) {
    Write-Output 'Initializing a fresh disposable MySQL data directory...'
    & $mysqld --no-defaults --initialize-insecure "--basedir=$($mysqlBin | Split-Path)" "--datadir=$($data.Replace('\','/'))" --console
    $initExit = $LASTEXITCODE
    if (!(Test-Path (Join-Path $data 'mysql.ibd'))) {
        throw "MySQL initialization failed with exit code $initExit; see the initialization output above."
    }
}
New-Item -ItemType Directory -Path (Split-Path $log) -Force | Out-Null

try {
    Write-Output "Starting isolated MySQL 8.0 on 127.0.0.1:$port (separate from port 3306)..."
    $basedir = Split-Path $mysqlBin
    $datadirArg = $data.Replace('\','/')
    $pidfile = (Join-Path $work 'mysqld.pid').Replace('\','/')
    $serverArgs = @('--no-defaults', ('--basedir="' + $basedir + '"'), ('--datadir="' + $datadirArg + '"'), "--port=$port", '--bind-address=127.0.0.1', '--mysqlx=0', ('--pid-file="' + $pidfile + '"'), '--console') -join ' '
    $server = Start-Process -FilePath $mysqld -ArgumentList $serverArgs -PassThru -RedirectStandardOutput (Join-Path $work 'mysqld-stdout.log') -RedirectStandardError (Join-Path $work 'mysqld-stderr.log')
    $clientArgs = @('--no-defaults','--protocol=TCP','--host=127.0.0.1',"--port=$port",'--user=root','--batch','--skip-column-names')
    $ready = $false
    for ($i = 0; $i -lt 45; $i++) {
        Start-Sleep -Seconds 1
        if ($server.HasExited) {
            $err = if (Test-Path (Join-Path $work 'mysqld-stderr.log')) { Get-Content (Join-Path $work 'mysqld-stderr.log') -Raw } else { '' }
            throw "Isolated MySQL exited during startup (exit $($server.ExitCode)). $err"
        }
        & $mysql @clientArgs -e 'SELECT VERSION();' *> (Join-Path $work 'ping.txt')
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    }
    if (!$ready) {
        $err = if (Test-Path (Join-Path $work 'mysqld-stderr.log')) { Get-Content (Join-Path $work 'mysqld-stderr.log') -Raw } else { '' }
        throw "Isolated MySQL did not become ready on port $port. $err"
    }
    $serverStarted = $true
    $version = (& $mysql @clientArgs -e 'SELECT VERSION();' | Out-String).Trim()
    Write-Output "Isolated server ready: MySQL $version"

    Write-Output 'Loading schema.sql into the disposable instance...'
    & $mysql @clientArgs "-e" "source $($root.Replace('\','/'))/database/schema.sql"
    if ($LASTEXITCODE -ne 0) { throw 'database/schema.sql failed against the disposable MySQL server.' }

    Write-Output 'Loading seed.sql into the disposable instance...'
    & $mysql @clientArgs "-e" "source $($root.Replace('\','/'))/database/seed.sql"
    if ($LASTEXITCODE -ne 0) { throw 'database/seed.sql failed against the disposable MySQL server.' }

    $tableCount = (& $mysql @clientArgs --database=learnloop_db -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';" | Out-String).Trim()
    Write-Output "Seeded isolated schema contains $tableCount base tables."
    if ($tableCount -ne '15') { throw "Expected 15 LearnLoop base tables in the isolated test schema but found $tableCount." }

    $env:LEARNLOOP_TEST_DB_URL = "jdbc:mysql://127.0.0.1:$port/learnloop_db?serverTimezone=UTC&characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true"
    $env:LEARNLOOP_TEST_DB_USER = 'root'
    $env:LEARNLOOP_TEST_DB_PASSWORD = ''

    Write-Output 'Running full Maven clean verify with opt-in DB integration tests enabled...'
    Push-Location $root
    try {
        & 'C:\Users\prath\tools\apache-maven-3.9.9\bin\mvn.cmd' -B clean verify 2>&1 | Tee-Object -FilePath $log
        $buildExit = $LASTEXITCODE
    } finally {
        Pop-Location
    }

    $total = 0; $fail = 0; $errors = 0; $skipped = 0
    if (Test-Path (Join-Path $root 'target\surefire-reports')) {
        Get-ChildItem (Join-Path $root 'target\surefire-reports') -Filter 'TEST-*.xml' | ForEach-Object {
            try {
                [xml]$x = Get-Content $_.FullName
                $total += [int]$x.testsuite.tests
                $fail += [int]$x.testsuite.failures
                $errors += [int]$x.testsuite.errors
                $skipped += [int]$x.testsuite.skipped
            } catch {}
        }
    }
    $passed = $total - $fail - $errors - $skipped
    $result = if ($buildExit -eq 0 -and $fail -eq 0 -and $errors -eq 0) { 'PASS' } else { 'FAIL' }
    $details = @(
        '# Phase 14 - Isolated MySQL Integration Results'
        ''
        "- Executed: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
        "- Disposable database server: MySQL $version on 127.0.0.1:$port, separate data directory"
        "- Base tables after schema and seed: $tableCount"
        "- Maven result: **$result** (exit code $buildExit)"
        "- JUnit totals: $total tests; $passed passed; $fail failures; $errors errors; $skipped skipped"
        "- Build log: docs/phase14-integration-build.log"
        '- Isolation: the suite used only the temporary MySQL instance. The existing MySQL service on port 3306 and its learnloop_db were not targeted.'
        ''
        '## Per-suite summary'
        ''
        '| Test suite | Tests | Failures | Errors | Skipped |'
        '|---|---:|---:|---:|---:|'
    )
    $testReports = Join-Path $root 'target\surefire-reports'
    if (Test-Path $testReports) {
        Get-ChildItem $testReports -Filter 'TEST-*.xml' | Sort-Object Name | ForEach-Object {
            try {
                [xml]$x = Get-Content $_.FullName
                $details += "| $($x.testsuite.name) | $($x.testsuite.tests) | $($x.testsuite.failures) | $($x.testsuite.errors) | $($x.testsuite.skipped) |"
            } catch {}
        }
    }
    $details += ''
    Set-Content -Path $report -Value $details -Encoding UTF8
    Write-Output "JUnit totals: $total tests; $passed passed; $fail failures; $errors errors; $skipped skipped."
    Write-Output "Integration report: $report"
    Write-Output "Build log: $log"
} finally {
    Remove-Item Env:LEARNLOOP_TEST_DB_URL -ErrorAction SilentlyContinue
    Remove-Item Env:LEARNLOOP_TEST_DB_USER -ErrorAction SilentlyContinue
    Remove-Item Env:LEARNLOOP_TEST_DB_PASSWORD -ErrorAction SilentlyContinue

    if ($server -and !$server.HasExited) {
        Write-Output 'Stopping only the disposable test MySQL process...'
        try {
            & $mysqlAdmin --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$port" --user=root shutdown *> $null
        } catch {}
        Start-Sleep -Seconds 2
        if (!$server.HasExited) {
            try { $server.Kill(); $server.WaitForExit(10000) } catch {}
        }
    }

    if ($serverStarted -and $server -and $server.HasExited) {
        Start-Sleep -Milliseconds 500
        try {
            Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction Stop
            Write-Output 'Disposable MySQL data directory removed.'
        } catch {
            Write-Warning "The disposable server stopped but its data directory could not be removed: $($_.Exception.Message)"
        }
    } else {
        Write-Warning "Disposable MySQL data directory kept for troubleshooting at $work."
    }
}

if ($buildExit -ne 0) { exit $buildExit }
