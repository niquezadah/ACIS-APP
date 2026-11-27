$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
function New-DemoSecret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    [Convert]::ToBase64String($bytes)
}
if (!(Test-Path -LiteralPath '.env')) {
    @("ACIS_ADMIN_EMAIL=admin@acis.example", "ACIS_ADMIN_PASSWORD=$(New-DemoSecret)",
      "ACIS_DB_PASSWORD=$(New-DemoSecret)", "ACIS_ACCOUNTS_PASSWORD=$(New-DemoSecret)") |
      Set-Content -Encoding ascii '.env'
}
docker compose up --build -d
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron iniciar los servicios. Revisa Docker Desktop.' }
Write-Output 'Servicios iniciados. La cuenta inicial está en el archivo privado .env.'
