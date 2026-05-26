$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendScript = Join-Path $Root "scripts\start-backend.ps1"
$FrontendScript = Join-Path $Root "scripts\start-frontend.ps1"
$AppUrl = "http://localhost:5173"
$LogsDir = Join-Path $Root "logs"

New-Item -ItemType Directory -Force $LogsDir | Out-Null

function Test-Port {
    param([int]$Port)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.BeginConnect("127.0.0.1", $Port, $null, $null)
        if (-not $connection.AsyncWaitHandle.WaitOne(500)) {
            return $false
        }

        $client.EndConnect($connection)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

Write-Host "Iniciando Acao Games..." -ForegroundColor Cyan

if (Test-Port 8080) {
    Write-Host "Backend ja esta rodando em http://localhost:8080" -ForegroundColor Yellow
} else {
    Start-Process -FilePath powershell.exe `
        -ArgumentList "-NoExit -ExecutionPolicy Bypass -File `"$BackendScript`"" `
        -WorkingDirectory $Root `
        -WindowStyle Normal
}

if (Test-Port 5173) {
    Write-Host "Frontend ja esta rodando em $AppUrl" -ForegroundColor Yellow
} else {
    Start-Process -FilePath powershell.exe `
        -ArgumentList "-NoExit -ExecutionPolicy Bypass -File `"$FrontendScript`"" `
        -WorkingDirectory $Root `
        -WindowStyle Normal
}

Write-Host "Abrindo navegador em $AppUrl..." -ForegroundColor Cyan
Start-Sleep -Seconds 8
Start-Process $AppUrl
