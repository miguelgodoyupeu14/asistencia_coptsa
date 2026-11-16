$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $projectRoot 'backend')
try { & .\mvnw.cmd test; if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed' } }
finally { Pop-Location }

Push-Location (Join-Path $projectRoot 'frontend-web')
try {
    & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Angular build failed' }
    & npm.cmd test -- --watch=false; if ($LASTEXITCODE -ne 0) { throw 'Angular tests failed' }
}
finally { Pop-Location }

Push-Location (Join-Path $projectRoot 'mobile')
try {
    & flutter.bat analyze; if ($LASTEXITCODE -ne 0) { throw 'Flutter analysis failed' }
    & flutter.bat test; if ($LASTEXITCODE -ne 0) { throw 'Flutter tests failed' }
}
finally { Pop-Location }

Write-Output 'All local validations passed.'
