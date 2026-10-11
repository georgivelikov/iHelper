<#
.SYNOPSIS
  Starts iHelper for local development: PostgreSQL (Docker), backend (:8080) and frontend (:5173).
.DESCRIPTION
  Each part is skipped if it is already running. Output goes to .run/backend.log and .run/frontend.log.
  Use scripts\stop.ps1 to stop everything.
#>
param(
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$runDir = Join-Path $root '.run'
New-Item -ItemType Directory -Force $runDir | Out-Null

function Test-Port([int]$port) {
    [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Until([string]$what, [scriptblock]$ready) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (& $ready) { Write-Host "  $what is up"; return }
        Start-Sleep -Seconds 2
    }
    throw "$what did not come up within $TimeoutSeconds s. See the logs in $runDir"
}

Write-Host 'PostgreSQL'
Push-Location $root
try { docker compose up -d postgres | Out-Null } finally { Pop-Location }
if ($LASTEXITCODE -ne 0) { throw 'docker compose failed. Is Docker Desktop running?' }
Wait-Until 'PostgreSQL' { (docker inspect -f '{{.State.Health.Status}}' ihelper-postgres 2>$null) -eq 'healthy' }

Write-Host 'Backend'
if (Test-Port 8080) {
    Write-Host '  already running on :8080'
} else {
    Start-Process -FilePath (Join-Path $root 'backend\mvnw.cmd') -ArgumentList 'spring-boot:run' `
        -WorkingDirectory (Join-Path $root 'backend') -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runDir 'backend.log') `
        -RedirectStandardError (Join-Path $runDir 'backend.err.log') | Out-Null
    Wait-Until 'Backend' {
        try { (Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health' -TimeoutSec 3).status -eq 'UP' } catch { $false }
    }
}

Write-Host 'Frontend'
if (Test-Port 5173) {
    Write-Host '  already running on :5173'
} else {
    $frontend = Join-Path $root 'frontend'
    if (-not (Test-Path (Join-Path $frontend 'node_modules'))) {
        Write-Host '  installing dependencies (first run)'
        Push-Location $frontend
        try { npm install | Out-Null } finally { Pop-Location }
    }
    Start-Process -FilePath 'npm.cmd' -ArgumentList 'run', 'dev' -WorkingDirectory $frontend -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runDir 'frontend.log') `
        -RedirectStandardError (Join-Path $runDir 'frontend.err.log') | Out-Null
    Wait-Until 'Frontend' { Test-Port 5173 }
}

Write-Host ''
Write-Host 'iHelper is running:  http://localhost:5173   (API: http://localhost:8080, Swagger: /swagger-ui)'
Write-Host 'Logs: .run\   Stop with: scripts\stop.ps1'
