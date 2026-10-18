@echo off
chcp 65001 >nul
echo 正在释放 8080 端口...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  taskkill /F /PID %%p >nul 2>&1
)
echo 已停止 CampusHub 后端。
pause
