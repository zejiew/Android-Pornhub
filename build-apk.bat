@echo off
REM ============================================================
REM  一键构建 APK（Windows）
REM
REM  前置条件：
REM    1) JDK 17（JAVA_HOME 指向它）
REM    2) Android SDK（ANDROID_HOME 或 local.properties 指定）
REM    3) Gradle 8.7+（本机已安装 gradle 命令，或用仓库里的 gradlew）
REM
REM  产物：app\build\outputs\apk\release\app-release.apk
REM ============================================================
setlocal enabledelayedexpansion

echo.
echo [1/4] 检查 Java...
where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 java，请安装 JDK 17 并配置 JAVA_HOME
    echo 下载：https://adoptium.net/temurin/releases/?version=17
    pause & exit /b 1
)
if "%JAVA_HOME%"=="" echo   [提示] JAVA_HOME 未设置，若构建报找不到 JDK 请手动设置

echo [2/4] 检查 Android SDK...
if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    ) else if exist "local.properties" (
        echo   使用 local.properties 中配置的 SDK
    ) else (
        echo [错误] 未找到 Android SDK。
        echo 请安装 Android Studio，或创建 local.properties 写入：
        echo   sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
        pause & exit /b 1
    )
)
if not "%ANDROID_HOME%"=="" echo   SDK = %ANDROID_HOME%

echo [3/4] 选择构建工具...
set "GRADLE_CMD="
if exist "gradlew.bat" (
    set "GRADLE_CMD=gradlew.bat"
    echo   使用仓库自带的 gradlew
) else (
    where gradle >nul 2>nul
    if not errorlevel 1 (
        set "GRADLE_CMD=gradle"
        echo   使用系统安装的 gradle
    ) else (
        echo [错误] 既没有 gradlew.bat，也没有安装 gradle。
        echo.
        echo   任选其一：
        echo     A. 安装 Gradle 8.7+：https://gradle.org/releases/
        echo     B. 装好 Gradle 后在本目录执行一次： gradle wrapper
        echo        （会生成 gradlew.bat 和 gradle-wrapper.jar）
        echo     C. 直接用 Android Studio 打开本目录，菜单 Build - Build APK
        echo     D. 推到 GitHub，用 Actions 云端构建（无需本地环境）
        pause & exit /b 1
    )
)

echo [4/4] 开始构建 release APK...
call %GRADLE_CMD% assembleRelease
if errorlevel 1 (
    echo.
    echo [失败] 构建未通过，请查看上方错误信息。常见原因：
    echo   1. 网络无法访问 maven / dl.google.com —— 配置代理或国内镜像
    echo   2. SDK 缺少 platforms;android-34 或 build-tools;34.0.0
    echo   3. JDK 版本不是 17
    pause & exit /b 1
)

echo.
echo [成功] APK 已生成：
echo   app\build\outputs\apk\release\app-release.apk
echo.
if exist "app\build\outputs\apk\release\app-release.apk" (
    explorer "app\build\outputs\apk\release"
)
pause
