@echo off
setlocal
cd /d %~dp0

rem Edit the credentials here if your MySQL differs
set DB_USER=root
set DB_PASS=123456
set MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
if not exist "%MYSQL%" set MYSQL=mysql

echo [1/3] Checking port 8080 ...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if not errorlevel 1 echo   Warning: a backend is running on 8080, the rebuild will interrupt it.

echo [2/3] Rebuilding database campushub ...
set SQLPATH=%~dp0init.sql
set SQLPATH=%SQLPATH:\=/%
"%MYSQL%" -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 -e "source %SQLPATH%"
if errorlevel 1 goto fail

echo [3/3] Done: 14 users / 6 categories / 32 items / 45 images / 5 bargains / 1 deal / 3 deal messages
pause
exit /b 0

:fail
echo Database rebuild failed. Check the MySQL service and the credentials in this file.
pause
exit /b 1
