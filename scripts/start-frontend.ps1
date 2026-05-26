$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FrontendDir = Join-Path $Root "frontend"
$LogDir = Join-Path $Root "logs"
$LogFile = Join-Path $LogDir "frontend.log"

New-Item -ItemType Directory -Force $LogDir | Out-Null
Set-Location $FrontendDir
Write-Host "Frontend Acao Games" -ForegroundColor Cyan
Write-Host "Log: $LogFile" -ForegroundColor DarkGray

if (-not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
    Write-Host "node_modules nao encontrado. Instalando dependencias..." -ForegroundColor Yellow
    npm.cmd install *>&1 | Tee-Object -FilePath $LogFile
}

Write-Host "Iniciando Vite em http://localhost:5173..." -ForegroundColor Green
npm.cmd run dev -- --host 0.0.0.0 --port 5173 *>&1 | Tee-Object -FilePath $LogFile -Append
