@echo off
setlocal
set FOUND=0
echo Stopping CampusHub backend ...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  echo   killing PID=%%p
  taskkill /F /T /PID %%p >nul 2>&1
  set FOUND=1
)
taskkill /F /FI "WINDOWTITLE eq CampusHub Backend*" >nul 2>&1
if "%FOUND%"=="1" (echo CampusHub backend stopped.) else (echo No process is using port 8080.)
pause
