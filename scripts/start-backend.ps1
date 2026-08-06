$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$BackendDir = Join-Path $Root "backend"
$MavenWrapper = Join-Path $BackendDir "mvnw.cmd"
$BackendJar = Join-Path $BackendDir "target\acaogames-backend-1.0.0.jar"
$DataDir = Join-Path $Root "data"
$LogDir = Join-Path $Root "logs"

New-Item -ItemType Directory -Force $DataDir | Out-Null
New-Item -ItemType Directory -Force $LogDir | Out-Null

$Port8080Proc = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
if ($Port8080Proc) {
    Write-Host "Encerrando processo anterior rodando na porta 8080 (PID: $Port8080Proc)..." -ForegroundColor Yellow
    Stop-Process -Id $Port8080Proc -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

Set-Location $BackendDir
Write-Host "Backend Acao Games" -ForegroundColor Cyan

if (Test-Path $BackendJar) {
    Write-Host "Iniciando pelo JAR..." -ForegroundColor Green
    java -jar $BackendJar
    exit $LASTEXITCODE
}

if (Test-Path $MavenWrapper) {
    Write-Host "Iniciando com Maven Wrapper..." -ForegroundColor Green
    & $MavenWrapper spring-boot:run
    exit $LASTEXITCODE
}

$Maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($Maven) {
    Write-Host "Iniciando com Maven..." -ForegroundColor Green
    mvn spring-boot:run
    exit $LASTEXITCODE
}

Write-Host "Nao foi possivel iniciar o backend." -ForegroundColor Red
Write-Host "Instale o Maven ou gere o arquivo backend\target\acaogames-backend-1.0.0.jar." -ForegroundColor Red
Read-Host "Pressione Enter para fechar"

