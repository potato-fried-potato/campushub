@echo off
chcp 65001 >nul
setlocal

rem 数据库账号密码，按本机修改
set DB_USER=root
set DB_PASS=123456
set MYSQL=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe
if not exist "%MYSQL%" set MYSQL=mysql

echo [1/2] 重建数据库 campushub 并导入演示数据...
"%MYSQL%" -u%DB_USER% -p%DB_PASS% --default-character-set=utf8mb4 -e "source %~dp0init.sql"
if errorlevel 1 goto fail

echo [2/2] 完成：14 用户 / 6 分类 / 32 商品 / 45 图 / 5 条砍价
pause
exit /b 0

:fail
echo 数据库重建失败，请检查 MySQL 服务与账号密码。
pause
exit /b 1
