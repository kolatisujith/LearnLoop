$ErrorActionPreference = 'Stop'

$root = 'C:\Users\prath\eclipse-workspace\LearnLoop'
$script = Join-Path $root 'scripts\phase14-browser-validation.cjs'
$node = (Get-Command node -ErrorAction Stop).Source

if (!(Test-Path $script)) {
    throw "Browser validation script not found: $script"
}

if (-not $env:PHASE14_BASE_URL) {
    $env:PHASE14_BASE_URL = 'http://localhost:8080/learnloop'
}

Write-Output 'Running Phase 14 browser validation with isolated browser contexts.'
Write-Output 'The test creates one uniquely named temporary material in class 2, then deletes it and verifies cleanup.'
Write-Output 'Admin routes are verified separately against a disposable database; this live run does not create admin accounts.'
& $node --check $script
if ($LASTEXITCODE -ne 0) {
    throw 'Browser validation JavaScript syntax check failed.'
}

& $node $script
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    Write-Output "Phase 14 browser validation exited with code $exitCode."
}
exit $exitCode
