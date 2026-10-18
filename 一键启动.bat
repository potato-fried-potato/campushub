@echo off
setlocal
cd /d %~dp0

set DB_USER=root
set DB_PASS=123456
set MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
if not exist "%MYSQL%" set MYSQL=mysql

echo [1/6] Checking JDK 21 ...
set JAVA_OK=0
if exist "%JAVA_HOME%\bin\java.exe" call :checkjava "%JAVA_HOME%\bin\java.exe"
if "%JAVA_OK%"=="0" call :checkjava java
if "%JAVA_OK%"=="0" goto nojava

echo [2/6] Checking MySQL service ...
sc query MySQL80 | findstr "RUNNING" >nul
if errorlevel 1 net start MySQL80 >nul 2>&1
"%MYSQL%" -u%DB_USER% -p%DB_PASS% -e "select 1" >nul 2>&1
if errorlevel 1 goto nomysql

echo [3/6] Checking database campushub ...
"%MYSQL%" -u%DB_USER% -p%DB_PASS% -e "use campushub" >nul 2>&1
if errorlevel 1 (
  echo   Database not found, importing db/init.sql ...
  call "%~dp0db\init-db.bat" < nul
)

echo [4/6] Checking frontend build ...
if not exist "backend\src\main\resources\static\index.html" goto buildfront
powershell -NoProfile -Command "$n=(Get-ChildItem 'frontend\src','frontend\index.html','frontend\vite.config.js' -Recurse -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime; $b=(Get-Item 'backend\src\main\resources\static\index.html').LastWriteTime; if($n -gt $b){exit 1}else{exit 0}"
if errorlevel 1 goto buildfront
goto backendbuild

:buildfront
echo   Building frontend (npm install + build) ...
pushd frontend
call npm install
call npm run build
if errorlevel 1 (
  popd
  goto fail
)
popd

:backendbuild
echo [5/6] Building backend (mvn package) ...
pushd backend
call mvn -q -DskipTests package
if errorlevel 1 (
  popd
  goto fail
)
popd

echo [6/6] Starting backend ...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 goto portbusy
start "CampusHub Backend" cmd /c "cd /d %~dp0backend && mvn spring-boot:run"
echo   Waiting for backend ready (max 90s) ...
set TRIES=0
:waitup
timeout /t 2 /nobreak >nul
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 goto up
set /a TRIES+=1
if %TRIES% lss 45 goto waitup
echo   Backend did not start in time. Check the "CampusHub Backend" window log.
pause
exit /b 1

:up
start "" http://localhost:8080
echo   OK: http://localhost:8080
echo Done.
exit /b 0

:portbusy
echo   Port 8080 is already in use. Please run the stop script first.
pause
exit /b 1

:nojava
echo   JDK 21 not found. Install JDK 21 and set JAVA_HOME (now: %JAVA_HOME%).
pause
exit /b 1

:nomysql
echo   Cannot connect to MySQL. Check the MySQL80 service and the DB_USER / DB_PASS in db\init-db.bat.
pause
exit /b 1

:fail
echo   Build failed, see the log above.
pause
exit /b 1

:checkjava
"%~1" -version 2>&1 | findstr "21." >nul
if not errorlevel 1 set JAVA_OK=1
goto :eof
