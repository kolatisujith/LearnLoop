# Phase 14 signed-in admin browser test on isolated database/Tomcat only.
$ErrorActionPreference = 'Stop'
$root = 'C:\Users\prath\eclipse-workspace\LearnLoop'
$mysqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$mysqld = Join-Path $mysqlBin 'mysqld.exe'
$mysql = Join-Path $mysqlBin 'mysql.exe'
$mysqlAdmin = Join-Path $mysqlBin 'mysqladmin.exe'
$mysqlBasedir = 'C:\Program Files\MySQL\MySQL Server 8.0'
$tomcatHome = 'C:\Users\prath\OneDrive\Documents\apache-tomcat-11.0.4'
$java = 'C:\Users\prath\tools\jdk-21\bin\java.exe'
$tomcatJava = 'C:\Users\prath\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.4.v20260826-0822\jre\bin\java.exe'
$mysqlConnector = 'C:\Users\prath\.m2\repository\com\mysql\mysql-connector-j\9.1.0\mysql-connector-j-9.1.0.jar'
$bcryptJar = 'C:\Users\prath\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar'
$wtpBase = 'C:\Users\prath\eclipse-workspace\.metadata\.plugins\org.eclipse.wst.server.core\tmp0'
$port = 3307
$httpPort = 8181
$shutdownPort = 18005
$work = Join-Path 'C:\Users\prath\eclipse-workspace' ('_LearnLoop_Phase14AdminBrowser_' + [guid]::NewGuid().ToString('N'))
$data = Join-Path $work 'mysql-data'
$testTomcatBase = Join-Path $work 'tomcat-base'
$materials = Join-Path $work 'materials'
$adminEmail = 'phase14-admin-' + [guid]::NewGuid().ToString('N') + '@learnloop.local'
$adminPassword = 'P14-' + [guid]::NewGuid().ToString('N') + '!aA9'
$adminName = 'Phase 14 Isolated Test Administrator'
$mysqlProcess = $null
$tomcatProcess = $null
$browserExit = 2
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($p in @($mysqld,$mysql,$mysqlAdmin,$java,$tomcatJava,$mysqlConnector,$bcryptJar,(Join-Path $tomcatHome 'bin\bootstrap.jar'),(Join-Path $wtpBase 'wtpwebapps\LearnLoop\WEB-INF\web.xml'))) {
    if (!(Test-Path $p)) { throw "Required runtime file not found: $p" }
}
foreach ($checkPort in @($port,$httpPort,$shutdownPort)) {
    if (Get-NetTCPConnection -State Listen -LocalPort $checkPort -ErrorAction SilentlyContinue) {
        throw "Port $checkPort is already in use; refusing to take over an unknown service."
    }
}

try {
    New-Item -ItemType Directory -Path $work,$data,$testTomcatBase,(Join-Path $testTomcatBase 'conf'),(Join-Path $testTomcatBase 'logs'),(Join-Path $testTomcatBase 'temp'),(Join-Path $testTomcatBase 'work'),(Join-Path $testTomcatBase 'webapps'),(Join-Path $testTomcatBase 'wtpwebapps'),$materials -Force | Out-Null
    Write-Output 'Initializing disposable MySQL data...'
    $initOut = Join-Path $work 'mysql-init-out.log'
    $initErr = Join-Path $work 'mysql-init-err.log'
    $initArgs = '--no-defaults --initialize-insecure "--basedir=' + $mysqlBasedir + '" "--datadir=' + $data.Replace('\','/') + '" --console'
    $initProcess = Start-Process -FilePath $mysqld -ArgumentList $initArgs -PassThru -Wait -RedirectStandardOutput $initOut -RedirectStandardError $initErr
    if ($initProcess.ExitCode -ne 0 -or !(Test-Path (Join-Path $data 'mysql.ibd'))) {
        $details = if (Test-Path $initErr) { Get-Content $initErr -Raw } else { '' }
        throw ('Could not initialize temporary MySQL data. ' + $details)
    }

    $mysqlArgs = '--no-defaults "--basedir=' + $mysqlBasedir + '" "--datadir=' + $data.Replace('\','/') + '" --port=' + $port + ' --bind-address=127.0.0.1 --mysqlx=0 "--pid-file=' + (Join-Path $work 'mysql.pid').Replace('\','/') + '" --console'
    $mysqlProcess = Start-Process -FilePath $mysqld -ArgumentList $mysqlArgs -PassThru -RedirectStandardOutput (Join-Path $work 'mysql-out.log') -RedirectStandardError (Join-Path $work 'mysql-err.log')
    $clientArgs = @('--no-defaults','--protocol=TCP','--host=127.0.0.1',"--port=$port",'--user=root','--batch','--skip-column-names')
    $ready = $false
    for ($i=0; $i -lt 45; $i++) {
        Start-Sleep -Seconds 1
        if ($mysqlProcess.HasExited) { throw 'Temporary MySQL exited before becoming ready.' }
        & $mysql @clientArgs -e 'SELECT VERSION();' *> (Join-Path $work 'mysql-ping.txt')
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    }
    if (!$ready) { throw 'Temporary MySQL did not become ready on port 3307.' }
    $version = (& $mysql @clientArgs -e 'SELECT VERSION();' | Out-String).Trim()
    Write-Output "Temporary database ready: MySQL $version."

    $schema = (Join-Path $root 'database\schema.sql').Replace('\','/')
    $seed = (Join-Path $root 'database\seed.sql').Replace('\','/')
    & $mysql @clientArgs -e "source $schema"
    if ($LASTEXITCODE -ne 0) { throw 'Schema load failed on the temporary database.' }
    & $mysql @clientArgs -e "source $seed"
    if ($LASTEXITCODE -ne 0) { throw 'Seed load failed on the temporary database.' }
    $tables = (& $mysql @clientArgs --database=learnloop_db -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE';" | Out-String).Trim()
    if ($tables -ne '15') { throw "Expected 15 tables in isolated schema; found $tables." }

    $env:LEARNLOOP_DB_URL = "jdbc:mysql://127.0.0.1:$port/learnloop_db?serverTimezone=UTC&characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true"
    $env:LEARNLOOP_DB_USER = 'root'
    $env:LEARNLOOP_DB_PASSWORD = ''
    $env:LEARNLOOP_ADMIN_EMAIL = $adminEmail
    $env:LEARNLOOP_ADMIN_NAME = $adminName
    $env:LEARNLOOP_ADMIN_PASSWORD = $adminPassword
    $env:JAVA_HOME = 'C:\Users\prath\tools\jdk-21'
    $cliClasspath = (Join-Path $root 'target\classes') + ';' + $mysqlConnector + ';' + $bcryptJar
    $cliUrl = "jdbc:mysql://127.0.0.1:$port/learnloop_db?serverTimezone=UTC&characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true"
    Write-Output 'Bootstrapping a temporary admin with the supported AdminBootstrapCli...'
    & $java "-Dlearnloop.db.url=$cliUrl" '-Dlearnloop.db.username=root' '-Dlearnloop.db.password=' '-classpath' $cliClasspath 'com.learnloop.cli.AdminBootstrapCli'
    $bootstrapExit = $LASTEXITCODE
    if ($bootstrapExit -ne 0) { throw 'AdminBootstrapCli failed against the disposable database.' }

    Remove-Item Env:LEARNLOOP_DB_URL,Env:LEARNLOOP_DB_USER,Env:LEARNLOOP_DB_PASSWORD,Env:LEARNLOOP_ADMIN_EMAIL,Env:LEARNLOOP_ADMIN_NAME,Env:LEARNLOOP_ADMIN_PASSWORD -ErrorAction SilentlyContinue

    Copy-Item (Join-Path $wtpBase 'conf\*') (Join-Path $testTomcatBase 'conf') -Recurse -Force
    $serverXmlPath = Join-Path $testTomcatBase 'conf\server.xml'
    $serverXml = Get-Content $serverXmlPath -Raw
    $serverXml = $serverXml.Replace('<Server port="8005" shutdown="SHUTDOWN">','<Server port="' + $shutdownPort + '" shutdown="SHUTDOWN">')
    $serverXml = $serverXml.Replace('<Connector connectionTimeout="20000" port="8080" protocol="HTTP/1.1" redirectPort="8443"/>','<Connector connectionTimeout="20000" port="' + $httpPort + '" protocol="HTTP/1.1" redirectPort="8443"/>')
    Set-Content -Path $serverXmlPath -Value $serverXml -Encoding UTF8

    $dbUrl = "jdbc:mysql://127.0.0.1:$port/learnloop_db?serverTimezone=UTC&characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true"
    $cp = '"' + $tomcatHome + '\bin\bootstrap.jar;' + $tomcatHome + '\bin\tomcat-juli.jar"'
    $javaArgs = '-Dcatalina.base="' + $testTomcatBase + '" -Dcatalina.home="' + $tomcatHome + '" -Dwpt.deploy="' + $wtpBase + '\wtpwebapps" -Djava.library.path="' + $tomcatHome + '\bin" --enable-native-access=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED -Dlearnloop.app.base-url=http://localhost:' + $httpPort + '/learnloop -Dlearnloop.db.url="' + $dbUrl + '" -Dlearnloop.db.username=root -Dlearnloop.db.password= -Dlearnloop.materials.dir="' + $materials + '" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -classpath ' + $cp + ' org.apache.catalina.startup.Bootstrap start'
    Write-Output 'Starting temporary Tomcat on port 8181 with the test database configured...'
    $tomcatProcess = Start-Process -FilePath $tomcatJava -ArgumentList $javaArgs -PassThru -WorkingDirectory $tomcatHome -RedirectStandardOutput (Join-Path $testTomcatBase 'logs\stdout.log') -RedirectStandardError (Join-Path $testTomcatBase 'logs\stderr.log')
    $webReady = $false
    for ($i=0; $i -lt 45; $i++) {
        Start-Sleep -Seconds 1
        try {
            $resp = Invoke-WebRequest "http://localhost:$httpPort/learnloop/health" -UseBasicParsing -TimeoutSec 3
            if ([int]$resp.StatusCode -eq 200) { $webReady = $true; break }
        } catch {}
        if ($tomcatProcess.HasExited) { throw 'Temporary Tomcat exited before becoming ready.' }
    }
    if (!$webReady) {
        $logText = if (Test-Path (Join-Path $testTomcatBase 'logs\stderr.log')) { Get-Content (Join-Path $testTomcatBase 'logs\stderr.log') -Tail 30 | Out-String } else { '' }
        throw "Temporary Tomcat did not become ready. $logText"
    }

    $env:PHASE14_BASE_URL = "http://localhost:$httpPort/learnloop"
    $env:PHASE14_ADMIN_EMAIL = $adminEmail
    $env:PHASE14_ADMIN_PASSWORD = $adminPassword
    Write-Output 'Running signed-in admin page checks against the isolated database...'
    & node (Join-Path $root 'scripts\phase14-admin-isolated-browser.cjs')
    $browserExit = $LASTEXITCODE
    if ($browserExit -ne 0) { $failures.Add("Admin browser checks exited with code $browserExit.") }

} catch {
    $failures.Add($_.Exception.Message)
    Write-Output ('Isolated admin browser test error: ' + $_.Exception.Message)
    foreach ($logPath in @((Join-Path $testTomcatBase 'logs\stderr.log'),(Join-Path $testTomcatBase 'logs\stdout.log'))) {
        if (Test-Path $logPath) {
            Write-Output ('--- temporary Tomcat log: ' + (Split-Path $logPath -Leaf) + ' ---')
            Get-Content $logPath -Tail 50
        }
    }
} finally {
    Remove-Item Env:LEARNLOOP_DB_URL,Env:LEARNLOOP_DB_USER,Env:LEARNLOOP_DB_PASSWORD,Env:LEARNLOOP_ADMIN_EMAIL,Env:LEARNLOOP_ADMIN_NAME,Env:LEARNLOOP_ADMIN_PASSWORD,Env:PHASE14_BASE_URL,Env:PHASE14_ADMIN_EMAIL,Env:PHASE14_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    if ($tomcatProcess -and !$tomcatProcess.HasExited) {
        try { Stop-Process -Id $tomcatProcess.Id -Force -ErrorAction SilentlyContinue } catch {}
    }
    if ($mysqlProcess -and !$mysqlProcess.HasExited) {
        try { & $mysqlAdmin --no-defaults --protocol=TCP --host=127.0.0.1 "--port=$port" --user=root shutdown *> $null } catch {}
        Start-Sleep -Seconds 2
        if (!$mysqlProcess.HasExited) { try { Stop-Process -Id $mysqlProcess.Id -Force -ErrorAction SilentlyContinue } catch {} }
    }
    Start-Sleep -Milliseconds 500
    $liveMysql = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'mysqld.exe' -and $_.CommandLine -and $_.CommandLine.Contains($work) }
    $liveTomcat = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -and $_.CommandLine.Contains($testTomcatBase) -and $_.CommandLine -match 'org\.apache\.catalina\.startup\.Bootstrap' }
    if (!$liveMysql -and !$liveTomcat -and (Test-Path $work)) {
        try { Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction Stop; Write-Output 'Temporary MySQL, Tomcat, logs and material storage removed.' }
        catch { $failures.Add('Temporary files were left for recovery because removal failed.'); Write-Warning "Temp work retained at $work" }
    } else {
        $failures.Add('Temporary server still appears to be running; its data was retained for safe recovery.')
        Write-Warning "Temp work retained at $work"
    }
}
Write-Output ("ISOLATED_ADMIN_BROWSER_EXIT=" + $browserExit)
if ($failures.Count -gt 0) { $failures | ForEach-Object { Write-Output ('- ' + $_) }; exit 1 }
if ($browserExit -ne 0) { exit $browserExit }
