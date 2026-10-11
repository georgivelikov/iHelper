<#
.SYNOPSIS
  Stops the local iHelper dev stack started by scripts\start.ps1.
.DESCRIPTION
  Stops the frontend (:5173) and backend (:8080) by port, then the PostgreSQL container.
  Data is kept in the ihelper-pgdata volume. Use -KeepDatabase to leave PostgreSQL running.
#>
param(
    [switch]$KeepDatabase
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot

function Stop-Port([int]$port, [string]$name) {
    $pids = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique
    if (-not $pids) { Write-Host "${name}: not running"; return }
    foreach ($id in $pids) {
        # /T also ends the child processes (npm, forked JVM); stopping by port never touches other Java/Node apps.
        taskkill /PID $id /T /F | Out-Null
    }
    Write-Host "${name}: stopped"
}

Stop-Port 5173 'Frontend'
Stop-Port 8080 'Backend'

if ($KeepDatabase) {
    Write-Host 'PostgreSQL: left running'
} else {
    Push-Location $root
    try { docker compose stop postgres | Out-Null; Write-Host 'PostgreSQL: stopped (data kept)' } finally { Pop-Location }
}
