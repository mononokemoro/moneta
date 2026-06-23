#Requires -Version 5.1
$ErrorActionPreference = 'SilentlyContinue'

$BackendPort = 5174
$FrontendPort = 5173
$bUp = $false
$fUp = $false

function Test-PortListening([int]$Port) {
  return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue).Count -gt 0
}

Write-Host '[check] Moneta 서비스 상태 확인'
Write-Host ''

if (Test-PortListening $BackendPort) {
  $bUp = $true
  try {
    $r = Invoke-WebRequest -Uri "http://localhost:$BackendPort/api/health" -UseBasicParsing -TimeoutSec 3
    if ($r.StatusCode -eq 200) {
      Write-Host "[check] 백엔드   : 실행 중 - 포트 $BackendPort, health OK"
      Write-Host "           URL http://localhost:$BackendPort/api/health"
    } else {
      Write-Host "[check] 백엔드   : 포트 $BackendPort 사용 중 - health HTTP $($r.StatusCode)"
    }
  } catch {
    Write-Host "[check] 백엔드   : 포트 $BackendPort 사용 중 - health 응답 없음"
  }
} else {
  Write-Host "[check] 백엔드   : 중지됨 - 포트 $BackendPort"
}

if (Test-PortListening $FrontendPort) {
  $fUp = $true
  Write-Host "[check] 프론트엔드: 실행 중 - 포트 $FrontendPort"
  Write-Host "          URL http://localhost:$FrontendPort/"
} else {
  Write-Host "[check] 프론트엔드: 중지됨 - 포트 $FrontendPort"
}

Write-Host ''
if ($bUp -and $fUp) {
  Write-Host '[check] 결과: 모두 실행 중'
  exit 0
}
if (-not $bUp -and -not $fUp) {
  Write-Host '[check] 결과: 모두 중지됨'
  Write-Host '          기동: run.cmd'
  exit 2
}
Write-Host '[check] 결과: 일부만 실행 중'
if (-not $bUp) { Write-Host '          백엔드 기동: restart.cmd' }
if (-not $fUp) { Write-Host '          프론트엔드 기동: run.cmd' }
exit 1
