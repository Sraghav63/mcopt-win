$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & .\gradlew.bat '-PtargetPlatform=windows' ':metal:build' ':fpshud:build' ':metal:windowsRelease' '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw "Windows build failed ($LASTEXITCODE)" }
    Get-ChildItem .\dist\mcopt-windows-*.jar, .\dist\mcopt-windows-*.zip
} finally {
    Pop-Location
}
