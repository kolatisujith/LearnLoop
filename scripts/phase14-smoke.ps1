# Phase 14: live, read-only-after-registration LearnLoop workflow smoke test.
# The only database write is a temporary student account, removed in the finally block.
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/learnloop'
$root = 'C:\Users\prath\eclipse-workspace\LearnLoop'
$reportPath = Join-Path $root 'docs\phase14-validation.md'
$results = [System.Collections.Generic.List[object]]::new()
$testEmail = $null
$testPassword = $null
$cleanupResult = 'not needed'
$cookies = New-Object System.Net.CookieContainer

function Add-Check([string]$Name, [bool]$Passed, [string]$Detail) {
    $script:results.Add([pscustomobject]@{
        Check = $Name
        Result = if ($Passed) { 'PASS' } else { 'FAIL' }
        Detail = $Detail
    })
}

function Send-Http([string]$Url, [string]$Method = 'GET', $Values = $null) {
    $request = [System.Net.HttpWebRequest]::Create($Url)
    $request.Method = $Method
    $request.CookieContainer = $script:cookies
    $request.AllowAutoRedirect = $true
    $request.Timeout = 15000
    $request.ReadWriteTimeout = 15000
    if ($null -ne $Values) {
        $pairs = foreach ($key in $Values.Keys) {
            [System.Net.WebUtility]::UrlEncode([string]$key) + '=' +
                [System.Net.WebUtility]::UrlEncode([string]$Values[$key])
        }
        $bytes = [System.Text.Encoding]::UTF8.GetBytes(($pairs -join '&'))
        $request.ContentType = 'application/x-www-form-urlencoded'
        $request.ContentLength = $bytes.Length
        $stream = $request.GetRequestStream()
        try { $stream.Write($bytes, 0, $bytes.Length) } finally { $stream.Dispose() }
    }
    try {
        $response = $request.GetResponse()
    } catch [System.Net.WebException] {
        if ($null -eq $_.Exception.Response) { throw }
        $response = $_.Exception.Response
    }
    try {
        $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
        try { $body = $reader.ReadToEnd() } finally { $reader.Dispose() }
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Url = $response.ResponseUri.AbsoluteUri
            Path = $response.ResponseUri.AbsolutePath
            Query = $response.ResponseUri.Query
            Headers = $response.Headers
            Body = $body
        }
    } finally {
        $response.Dispose()
    }
}

function Get-Csrf([string]$Html) {
    $match = [regex]::Match($Html, 'name="_csrf"\s+value="([^"]+)"')
    if (!$match.Success) { throw 'Expected hidden CSRF field was not found.' }
    return $match.Groups[1].Value
}

function Test-AllowedRoute([string]$Path) {
    try {
        $r = Send-Http ($script:base + $Path)
        $ok = ($r.Status -eq 200 -and $r.Path.EndsWith('/learnloop' + $Path, [System.StringComparison]::OrdinalIgnoreCase))
        Add-Check "Student GET $Path" $ok "HTTP $($r.Status); final path $($r.Path)"
    } catch {
        Add-Check "Student GET $Path" $false $_.Exception.Message
    }
}

function Test-DeniedRoute([string]$Path, [string]$Label = '') {
    try {
        $r = Send-Http ($script:base + $Path)
        $ok = ($r.Status -eq 403 -or $r.Status -eq 404)
        $name = if ($Label) { $Label } else { "Student denied $Path" }
        Add-Check $name $ok "HTTP $($r.Status); final path $($r.Path)"
    } catch {
        $name = if ($Label) { $Label } else { "Student denied $Path" }
        Add-Check $name $false $_.Exception.Message
    }
}

try {
    # Public endpoints and security defaults.
    $health = Send-Http ($base + '/health')
    Add-Check 'Public health endpoint' ($health.Status -eq 200 -and $health.Body -match '"status"\s*:\s*"UP"') "HTTP $($health.Status)"
    $homePage = Send-Http ($base + '/')
    Add-Check 'Landing page and CSP' ($homePage.Status -eq 200 -and $homePage.Headers['Content-Security-Policy'] -match "default-src 'self'") "HTTP $($homePage.Status); CSP present=$([bool]$homePage.Headers['Content-Security-Policy'])"
    $loginPage = Send-Http ($base + '/login')
    Add-Check 'Login page and no-store headers' ($loginPage.Status -eq 200 -and $loginPage.Headers['Cache-Control'] -match 'no-store') "HTTP $($loginPage.Status); Cache-Control=$($loginPage.Headers['Cache-Control'])"
    $registerPage = Send-Http ($base + '/register')
    Add-Check 'Registration page has CSRF token' ($registerPage.Status -eq 200 -and $registerPage.Body -match 'name="_csrf"') "HTTP $($registerPage.Status)"
    $privateView = Send-Http ($base + '/WEB-INF/views/auth/login.jsp')
    Add-Check 'WEB-INF view cannot be fetched directly' ($privateView.Status -eq 404 -or $privateView.Status -eq 403) "HTTP $($privateView.Status)"

    foreach ($path in @('/student/dashboard', '/teacher/dashboard', '/admin/dashboard')) {
        $r = Send-Http ($base + $path)
        Add-Check "Unauthenticated $path requires sign-in" ($r.Path.EndsWith('/learnloop/login') -and $r.Status -eq 200) "HTTP $($r.Status); final path $($r.Path)"
    }

    # Register a uniquely named disposable student through the real web form.
    $guid = [guid]::NewGuid().ToString('N')
    $testEmail = "phase14.$guid@learnloop.local"
    $testPassword = 'LLPhase14-' + [guid]::NewGuid().ToString('N') + 'aA9!'
    $register = Send-Http ($base + '/register')
    $csrf = Get-Csrf $register.Body
    $created = Send-Http ($base + '/register') 'POST' @{
        _csrf = $csrf
        fullName = 'Phase 14 Temporary Student'
        role = 'STUDENT'
        email = $testEmail
        password = $testPassword
        confirmPassword = $testPassword
    }
    $registrationOk = ($created.Status -eq 200 -and $created.Path.EndsWith('/learnloop/login') -and $created.Body -match 'Your student account was created')
    Add-Check 'Student registration workflow' $registrationOk "HTTP $($created.Status); final path $($created.Path)"

    # Sign in using the newly registered account; no existing credentials are used.
    $signInPage = Send-Http ($base + '/login')
    $loginCsrf = Get-Csrf $signInPage.Body
    $beforeCookie = @($cookies.GetCookies([uri]$base) | Where-Object Name -eq 'JSESSIONID' | Select-Object -First 1).Value
    $signedIn = Send-Http ($base + '/login') 'POST' @{
        _csrf = $loginCsrf
        email = $testEmail
        password = $testPassword
    }
    $loginOk = ($signedIn.Status -eq 200 -and $signedIn.Path.EndsWith('/learnloop/student/dashboard'))
    $afterCookie = @($cookies.GetCookies([uri]$base) | Where-Object Name -eq 'JSESSIONID' | Select-Object -First 1).Value
    Add-Check 'Student sign-in and dashboard redirect' $loginOk "HTTP $($signedIn.Status); final path $($signedIn.Path)"
    Add-Check 'Session ID rotated after sign-in' ([bool]$beforeCookie -and [bool]$afterCookie -and $beforeCookie -ne $afterCookie) "pre-login cookie present=$([bool]$beforeCookie); post-login cookie present=$([bool]$afterCookie)"

    if ($loginOk) {
        foreach ($path in @(
            '/student/dashboard', '/student/classes', '/student/requests',
            '/student/discover', '/student/learning', '/student/assessments',
            '/student/analytics'
        )) { Test-AllowedRoute $path }

        Test-DeniedRoute '/teacher/dashboard' 'Student cannot open teacher dashboard'
        Test-DeniedRoute '/admin/dashboard' 'Student cannot open admin dashboard'
        Test-DeniedRoute '/student/classes/2' 'Student cannot read a class without enrollment'

        $getLogout = Send-Http ($base + '/logout')
        Add-Check 'GET logout is rejected' ($getLogout.Status -eq 405) "HTTP $($getLogout.Status)"

        $postLogoutWithoutCsrf = Send-Http ($base + '/logout') 'POST' @{}
        Add-Check 'POST logout without CSRF is rejected' ($postLogoutWithoutCsrf.Status -eq 403) "HTTP $($postLogoutWithoutCsrf.Status)"

        $dashboardAfterRejectedLogout = Send-Http ($base + '/student/dashboard')
        Add-Check 'Rejected logout leaves session valid' ($dashboardAfterRejectedLogout.Status -eq 200 -and $dashboardAfterRejectedLogout.Path.EndsWith('/learnloop/student/dashboard')) "HTTP $($dashboardAfterRejectedLogout.Status); final path $($dashboardAfterRejectedLogout.Path)"
    }
} catch {
    Add-Check 'Unexpected smoke-test exception' $false $_.Exception.Message
} finally {
    # Remove only the temporary account just generated by this script.
    if ($testEmail -and $testEmail -match '^phase14\.[0-9a-f]{32}@learnloop\.local$') {
        try {
            $props = @{}
            Get-Content (Join-Path $root 'src\main\resources\db.properties') |
                Where-Object { $_ -match '^\s*[^#!\s][^=]*=' } |
                ForEach-Object {
                    $parts = $_.Split('=', 2)
                    $props[$parts[0].Trim()] = $parts[1].Trim()
                }
            $env:MYSQL_PWD = $props['db.password']
            $mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
            $sql = "DELETE FROM users WHERE email = '$testEmail'; SELECT ROW_COUNT();"
            $cleanupOutput = & $mysql --protocol=TCP --host=localhost --port=3306 "--user=$($props['db.username'])" --database=learnloop_db --batch --skip-column-names -e $sql 2>&1
            if ($LASTEXITCODE -eq 0) {
                $remaining = & $mysql --protocol=TCP --host=localhost --port=3306 "--user=$($props['db.username'])" --database=learnloop_db --batch --skip-column-names -e "SELECT COUNT(*) FROM users WHERE email = '$testEmail';" 2>&1
                if ($LASTEXITCODE -eq 0 -and (($remaining -join '').Trim() -eq '0')) {
                    $cleanupResult = 'temporary student removed; zero matching rows remain'
                    Add-Check 'Temporary account cleanup' $true 'remaining matching users=0'
                } else {
                    $cleanupResult = 'delete ran but verification did not confirm removal'
                    Add-Check 'Temporary account cleanup' $false (($remaining | Out-String).Trim())
                }
            } else {
                $cleanupResult = 'cleanup command failed; inspect MySQL permissions'
                Add-Check 'Temporary account cleanup' $false ($cleanupOutput | Out-String).Trim()
            }
        } catch {
            $cleanupResult = 'cleanup failed: ' + $_.Exception.Message
            Add-Check 'Temporary account cleanup' $false $cleanupResult
        } finally {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
    }

    $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'
    $passed = @($results | Where-Object Result -eq 'PASS').Count
    $failed = @($results | Where-Object Result -eq 'FAIL').Count
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add('# Phase 14 - End-to-End Validation')
    $lines.Add('')
    $lines.Add("- Executed: $stamp")
    $lines.Add("- Base URL: $base")
    $lines.Add("- Result: **$passed passed, $failed failed**")
    $lines.Add("- Cleanup: $cleanupResult")
    $lines.Add('')
    $lines.Add('| Check | Result | Detail |')
    $lines.Add('|---|---|---|')
    foreach ($item in $results) {
        $detail = ([string]$item.Detail -replace '\|', '\|' -replace '[\r\n]+', ' ')
        $lines.Add("| $($item.Check) | $($item.Result) | $detail |")
    }
    $lines.Add('')
    $lines.Add('This script exercises public routes, registration, student authentication, protected route access, role isolation, session rotation, and CSRF rejection over HTTP. It does not reset the database or mutate existing classroom and assessment records.')
    $lines.Add('')
    $lines.Add('## Cross-suite Phase 14 validation')
    $lines.Add('- Isolated MySQL integration: `docs/phase14-integration-result.md` records 163/163 tests passing with no failures, errors or skips on a disposable MySQL 8.0.46 instance; port-3306 `learnloop_db` was not targeted.')
    $lines.Add('- Interactive browser coverage: `docs/phase14-browser-validation.md` records 26/26 checks passing, including teacher routes, progress analytics, owner file download headers, anonymous download denial, landing navigation/status, Jitsi iframe/CSP, and no uncaught JavaScript errors.')
    $lines.Add('- Admin coverage: `docs/phase14-admin-isolated-browser.md` records 15/15 signed-in admin checks passing against a disposable database/Tomcat; those temporary resources were removed.')
    $lines.Add('- Production readiness still requires environment-specific HTTPS configuration and a professional security review; the public Jitsi demo service remains prototype-only.')
    Set-Content -Path $reportPath -Value $lines -Encoding UTF8
}
$results | Format-Table -AutoSize
"PHASE14_REPORT=$reportPath"
"TOTAL=$($results.Count) PASS=$(@($results | Where-Object Result -eq 'PASS').Count) FAIL=$(@($results | Where-Object Result -eq 'FAIL').Count) CLEANUP=$cleanupResult"
if (@($results | Where-Object Result -eq 'FAIL').Count -gt 0) { exit 1 }
