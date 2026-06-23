@echo off

chcp 65001 >nul

setlocal EnableExtensions

cd /d "%~dp0"



set "BACKEND_PORT=5174"

set "FRONTEND_PORT=5173"



echo [stop] Moneta 서비스 중지 중...



call :stop_port %FRONTEND_PORT% "프론트엔드"

call :stop_port %BACKEND_PORT% "백엔드"



taskkill /FI "WINDOWTITLE eq Moneta Frontend*" /F >nul 2>&1

taskkill /FI "WINDOWTITLE eq Moneta Backend*" /F >nul 2>&1



echo [stop] 완료

exit /b 0



:stop_port

powershell -NoProfile -Command ^

  "$port = %1; $label = '%~2';" ^

  "$conns = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue);" ^

  "if ($conns.Count -eq 0) { Write-Host ('[stop] ' + $label + ' (포트 ' + $port + ') - 실행 중 아님'); exit 0 };" ^

  "$procIds = $conns | Select-Object -ExpandProperty OwningProcess -Unique;" ^

  "foreach ($procId in $procIds) { Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue };" ^

  "Write-Host ('[stop] ' + $label + ' (포트 ' + $port + ') 중지')"

exit /b 0

