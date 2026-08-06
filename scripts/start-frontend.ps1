$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FrontendDir = Join-Path $Root "frontend"
$LogDir = Join-Path $Root "logs"
$LogFile = Join-Path $LogDir "frontend.log"

New-Item -ItemType Directory -Force $LogDir | Out-Null

$Port5173Proc = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
if (-not $Port5173Proc) {
    $Port5173Proc = netstat -ano -p tcp |
        Select-String "^\s*TCP\s+\S+:5173\s+\S+\s+LISTENING\s+(\d+)\s*$" |
        ForEach-Object { [int]$_.Matches[0].Groups[1].Value } |
        Select-Object -Unique
}

if ($Port5173Proc) {
    Write-Host "Encerrando processo anterior rodando na porta 5173 (PID: $Port5173Proc)..." -ForegroundColor Yellow
    Stop-Process -Id $Port5173Proc -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

Set-Location $FrontendDir
Write-Host "Frontend Acao Games" -ForegroundColor Cyan
Write-Host "Log: $LogFile" -ForegroundColor DarkGray

if (-not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
    Write-Host "node_modules nao encontrado. Instalando dependencias..." -ForegroundColor Yellow
    npm.cmd install *>&1 | Tee-Object -FilePath $LogFile
}

$BuildIndex = Join-Path $FrontendDir "dist\index.html"
if (-not (Test-Path $BuildIndex)) {
    Write-Host "Build nao encontrado. Compilando o frontend uma unica vez..." -ForegroundColor Yellow
    npm.cmd run build *>&1 | Tee-Object -FilePath $LogFile -Append
}

Write-Host "Iniciando frontend compilado em http://localhost:5173..." -ForegroundColor Green
npm.cmd run preview -- --host 0.0.0.0 --port 5173 --strictPort
