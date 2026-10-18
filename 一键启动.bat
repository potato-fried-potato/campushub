@echo off
chcp 65001 >nul
setlocal
cd /d %~dp0

set DB_USER=root
set DB_PASS=123456
set MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
if not exist "%MYSQL%" set MYSQL=mysql

echo [1/6] 检查 JDK 21...
java -version 2>&1 | findstr "21." >nul
if errorlevel 1 goto nojava

echo [2/6] 检查 MySQL80 服务...
sc query MySQL80 | findstr "RUNNING" >nul
if errorlevel 1 (
  echo 尝试启动 MySQL80 服务...
  net start MySQL80 >nul 2>&1
)
"%MYSQL%" -u%DB_USER% -p%DB_PASS% -e "select 1" >nul 2>&1
if errorlevel 1 goto nomysql

echo [3/6] 检查数据库...
"%MYSQL%" -u%DB_USER% -p%DB_PASS% -e "use campushub" >nul 2>&1
if errorlevel 1 (
  echo 数据库不存在，自动导入 init.sql...
  "%MYSQL%" -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 -e "source db/init.sql"
  if errorlevel 1 goto fail
)

echo [4/6] 检查前端产物...
if not exist "backend\src\main\resources\static\index.html" goto buildfront
for /f %%f in ('dir /b /s frontend\src frontend\index.html frontend\vite.config.js 2^>nul') do (
  if "%%~tf" GTR "backend\src\main\resources\static\index.html" goto buildfront
)
goto backendbuild

:buildfront
echo 构建前端...
pushd frontend
call npm install >nul 2>&1
call npm run build
if errorlevel 1 (
  popd
  goto fail
)
popd

:backendbuild
echo [5/6] 构建后端...
pushd backend
call mvn -q -DskipTests package
if errorlevel 1 (
  popd
  goto fail
)
popd

echo [6/6] 启动后端并打开浏览器...
start "CampusHub Backend" cmd /c "cd /d %~dp0backend && mvn spring-boot:run"
timeout /t 12 /nobreak >nul
start "" http://localhost:8080
echo 启动完成：http://localhost:8080
exit /b 0

:nojava
echo 未检测到 JDK 21，请先安装 JDK 21 并配置 JAVA_HOME。
pause
exit /b 1

:nomysql
echo MySQL 无法连接，请检查 MySQL80 服务与 db\init-db.bat 中的账号密码。
pause
exit /b 1

:fail
echo 启动失败，请查看上方日志。
pause
exit /b 1
