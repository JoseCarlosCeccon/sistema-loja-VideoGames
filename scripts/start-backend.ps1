$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$BackendDir = Join-Path $Root "backend"
$MavenWrapper = Join-Path $BackendDir "mvnw.cmd"
$BackendJar = Join-Path $BackendDir "target\acaogames-backend-1.0.0.jar"
$DataDir = Join-Path $Root "data"
$LogDir = Join-Path $Root "logs"
$LogFile = Join-Path $LogDir "backend.log"

New-Item -ItemType Directory -Force $DataDir | Out-Null
New-Item -ItemType Directory -Force $LogDir | Out-Null
Set-Location $BackendDir
Write-Host "Backend Acao Games" -ForegroundColor Cyan
Write-Host "Log: $LogFile" -ForegroundColor DarkGray

if (Test-Path $BackendJar) {
    Write-Host "Iniciando pelo JAR (modo mais leve)..." -ForegroundColor Green
    java -jar $BackendJar *>&1 | Tee-Object -FilePath $LogFile
    exit $LASTEXITCODE
}

if (Test-Path $MavenWrapper) {
    Write-Host "Iniciando com Maven Wrapper..." -ForegroundColor Green
    & $MavenWrapper spring-boot:run *>&1 | Tee-Object -FilePath $LogFile
    exit $LASTEXITCODE
}

$Maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($Maven) {
    Write-Host "Iniciando com Maven..." -ForegroundColor Green
    mvn spring-boot:run *>&1 | Tee-Object -FilePath $LogFile
    exit $LASTEXITCODE
}

Write-Host "Nao foi possivel iniciar o backend." -ForegroundColor Red
Write-Host "Instale o Maven ou gere o arquivo backend\target\acaogames-backend-1.0.0.jar." -ForegroundColor Red
Read-Host "Pressione Enter para fechar"
