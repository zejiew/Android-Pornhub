@echo off
REM ============================================================
REM  一键构建 APK（Windows）
REM  首次运行会自动安装 Gradle Wrapper 所需的 Gradle 发行版
REM  产物路径：app\build\outputs\apk\release\app-release.apk
REM ============================================================
setlocal

echo.
echo [1/3] 检查 Java...
where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 java，请先安装 JDK 17 并配置 JAVA_HOME
    echo 下载：https://adoptium.net/temurin/releases/?version=17
    pause
    exit /b 1
)

echo [2/3] 检查 Android SDK...
if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
    ) else (
        echo [错误] 未找到 Android SDK。
        echo 请安装 Android Studio，或在 local.properties 中写入：
        echo   sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
        pause
        exit /b 1
    )
)
echo    SDK = %ANDROID_HOME%%ANDROID_SDK_ROOT%

echo [3/3] 开始构建 release APK...
call gradlew.bat assembleRelease
if errorlevel 1 (
    echo.
    echo [失败] 构建未通过，请查看上方错误信息。
    echo 常见原因：
    echo   1. 网络无法访问 maven / dl.google.com —— 用代理或配置国内镜像
    echo   2. SDK 缺少 platforms;android-34 或 build-tools;34.0.0
    echo   3. JDK 版本不是 17
    pause
    exit /b 1
)

echo.
echo [成功] APK 已生成：
echo   app\build\outputs\apk\release\app-release.apk
echo.
if exist "app\build\outputs\apk\release\app-release.apk" (
    explorer "app\build\outputs\apk\release"
)
pause
