@echo off
REM ============================================================
REM  推送代码到 GitHub
REM
REM  用法：双击运行，或在终端里执行 push-to-github.bat
REM
REM  第一次推送会要求输入用户名和密码：
REM    用户名 = zejiew
REM    密码   = 你的 Personal Access Token（不是 GitHub 登录密码！）
REM
REM  没有 Token 的话：
REM    1. 打开 https://github.com/settings/tokens
REM    2. Generate new token (classic)
REM    3. 勾选 repo 权限，生成后复制那串 ghp_xxxxx
REM    4. 粘贴到这里的密码位置
REM ============================================================
setlocal

cd /d "%~dp0"

echo.
echo ============ 当前状态 ============
git log --oneline
echo.
echo 远程仓库：
git remote -v
echo.

echo ============ 开始推送 ============
git push -u origin main
if errorlevel 1 (
    echo.
    echo [推送失败] 常见原因：
    echo   1. 密码位置填的是 GitHub 登录密码 —— 必须用 Personal Access Token
    echo   2. Token 没有勾选 repo 权限
    echo   3. 代理没开（本仓库配置的代理是 127.0.0.1:10808）
    echo   4. 仓库名/用户名拼错
    echo.
    pause & exit /b 1
)

echo.
echo [成功] 代码已推送！
echo.
echo 接下来：
echo   1. 打开 https://github.com/zejiew/Android-Pornhub/actions
echo   2. 等待 "Build APK" 工作流运行完成（约 3-6 分钟）
echo   3. 点进这次运行，页面底部 Artifacts 里下载 app-release-apk.zip
echo   4. 解压得到 app-release.apk，传到手机安装
echo.
pause
