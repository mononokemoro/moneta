@echo off

chcp 65001 >nul

setlocal EnableExtensions

cd /d "%~dp0"



set "BACKEND_PORT=5174"

set "FRONTEND_PORT=5173"



call :is_listening %BACKEND_PORT%

if not errorlevel 1 (

  echo [run] 백엔드가 이미 포트 %BACKEND_PORT% 에서 실행 중입니다.

  goto :start_frontend

)



call :is_listening %FRONTEND_PORT%

if not errorlevel 1 (

  echo [run] 프론트엔드가 이미 포트 %FRONTEND_PORT% 에서 실행 중입니다.

  goto :start_backend

)



:start_backend

call :is_listening %BACKEND_PORT%

if errorlevel 1 (

  echo [run] 백엔드 시작 중 ^(포트 %BACKEND_PORT%^)...

  start "Moneta Backend" cmd /k "cd /d ""%~dp0backend"" && gradlew.bat bootRun"

  timeout /t 3 /nobreak >nul

) else (

  echo [run] 백엔드가 이미 포트 %BACKEND_PORT% 에서 실행 중입니다.

)



:start_frontend

call :is_listening %FRONTEND_PORT%

if errorlevel 1 (

  if not exist "%~dp0frontend\node_modules\" (

    echo [run] frontend 의존성 설치 중 ^(npm install^)...

    pushd "%~dp0frontend"

    call npm install

    if errorlevel 1 (

      echo [run] npm install 실패

      popd

      exit /b 1

    )

    popd

  )



  echo [run] 프론트엔드 시작 중 ^(포트 %FRONTEND_PORT%^)...

  start "Moneta Frontend" cmd /k "cd /d ""%~dp0frontend"" && npm run dev"

) else (

  echo [run] 프론트엔드가 이미 포트 %FRONTEND_PORT% 에서 실행 중입니다.

)



echo.

echo [run] 기동 요청 완료

echo   Backend : http://localhost:%BACKEND_PORT%/api/health

echo   Frontend: http://localhost:%FRONTEND_PORT%/

echo   중지    : stop.cmd

exit /b 0



:is_listening

powershell -NoProfile -Command "$c = @(Get-NetTCPConnection -LocalPort %1 -State Listen -ErrorAction SilentlyContinue).Count; if ($c -gt 0) { exit 0 } else { exit 1 }"

exit /b %ERRORLEVEL%

