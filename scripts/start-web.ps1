$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath (Join-Path $projectRoot 'web')
$env:NEXT_PUBLIC_API_URL = 'http://127.0.0.1:8181'
if (-not (Test-Path -LiteralPath 'node_modules')) { npm ci --offline=false }
npm run dev -- --hostname 127.0.0.1 --port 3100
