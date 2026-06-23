@echo off

chcp 65001 >nul

setlocal EnableExtensions

cd /d "%~dp0"

set "BACKEND_PORT=5174"

echo [restart] 백엔드만 중지 후 재시작합니다.

call :stop_port %BACKEND_PORT% "백엔드"

taskkill /FI "WINDOWTITLE eq Moneta Backend*" /F >nul 2>&1

timeout /t 2 /nobreak >nul

call :is_listening %BACKEND_PORT%
if not errorlevel 1 (
  echo [restart] 포트 %BACKEND_PORT% 가 아직 사용 중입니다. 잠시 후 다시 시도하세요.
  exit /b 1
)

echo [restart] 백엔드 시작 중 ^(포트 %BACKEND_PORT%^)...
start "Moneta Backend" cmd /k "cd /d ""%~dp0backend"" && gradlew.bat bootRun"

timeout /t 3 /nobreak >nul

echo.
echo [restart] 완료
echo   Backend : http://localhost:%BACKEND_PORT%/api/health
echo   Frontend: 그대로 유지 ^(포트 5173^)
exit /b 0

:stop_port
powershell -NoProfile -Command ^
  "$port = %1; $label = '%~2';" ^
  "$conns = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue);" ^
  "if ($conns.Count -eq 0) { Write-Host ('[restart] ' + $label + ' (포트 ' + $port + ') - 실행 중 아님'); exit 0 };" ^
  "$procIds = $conns | Select-Object -ExpandProperty OwningProcess -Unique;" ^
  "foreach ($procId in $procIds) { Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue };" ^
  "Write-Host ('[restart] ' + $label + ' (포트 ' + $port + ') 중지')"
exit /b 0

:is_listening
powershell -NoProfile -Command "$c = @(Get-NetTCPConnection -LocalPort %1 -State Listen -ErrorAction SilentlyContinue).Count; if ($c -gt 0) { exit 0 } else { exit 1 }"
exit /b %ERRORLEVEL%
