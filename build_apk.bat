@echo off
setlocal enabledelayedexpansion

title FinPlay Pro - One-Click APK Builder

echo =====================================================================
echo           FINPLAY PRO - ANDROID APK BUILD SYSTEM (V1.0)
echo =====================================================================
echo.

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

:: 1. Check for Java / JDK / JBR
echo [1/3] Checking Java Development Kit (JDK)...
if not defined JAVA_HOME (
    if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
        set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
        echo  Found Android Studio JBR: !JAVA_HOME!
    ) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
        echo  Found JDK 17: !JAVA_HOME!
    ) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
        echo  Found JDK 21: !JAVA_HOME!
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo  JAVA_HOME is set to: %JAVA_HOME%
) else (
    where java >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo  Java detected in system PATH.
    ) else (
        echo  [WARNING] Java JDK not automatically found.
        echo  Please install JDK 17+ or open this project in Android Studio.
    )
)

:: 2. Check for Android SDK
echo.
echo [2/3] Checking Android SDK...
if not defined ANDROID_HOME (
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
        echo  Found Android SDK at: !ANDROID_HOME!
    )
)

if defined ANDROID_HOME (
    echo  ANDROID_HOME is set to: %ANDROID_HOME%
    if not exist "%SCRIPT_DIR%local.properties" (
        set "ESCAPED_SDK=!ANDROID_HOME:\=\\!"
        echo sdk.dir=!ESCAPED_SDK!> "%SCRIPT_DIR%local.properties"
        echo  Created local.properties with sdk.dir
    )
) else (
    echo  [NOTE] If building via command line, Android SDK will be read from local.properties
    echo         or Android Studio.
)

:: 3. Interactive Menu
echo.
echo =====================================================================
echo Select an option:
echo  [1] Build DEBUG APK (Fastest, ready to install on Android phone)
echo  [2] Build RELEASE APK (Optimized production build)
echo  [3] Clean & Rebuild APK
echo  [4] Run Local Node.js Web Server (Play Warland & games in browser)
echo  [5] Open Project in Android Studio
echo  [6] Open Output APK Directory
echo  [0] Exit
echo =====================================================================
set /p choice="Enter your choice (1-6, 0 to exit): "

if "%choice%"=="1" goto build_debug
if "%choice%"=="2" goto build_release
if "%choice%"=="3" goto clean_rebuild
if "%choice%"=="4" goto run_server
if "%choice%"=="5" goto open_studio
if "%choice%"=="6" goto open_output
if "%choice%"=="0" goto exit_script

echo Invalid choice. Defaulting to Debug APK build.
goto build_debug

:build_debug
echo.
echo =====================================================================
echo Building FinPlay Pro DEBUG APK...
echo =====================================================================
call gradlew.bat assembleDebug
if %ERRORLEVEL% equ 0 (
    echo.
    echo =====================================================================
    echo [SUCCESS] Debug APK built successfully!
    echo Output APK location:
    echo %SCRIPT_DIR%app\build\outputs\apk\debug\app-debug.apk
    echo =====================================================================
    explorer "%SCRIPT_DIR%app\build\outputs\apk\debug"
) else (
    echo.
    echo [FAILED] Command-line build failed. 
    echo Tip: Open this folder in Android Studio and click Build -> Build APK.
)
pause
goto exit_script

:build_release
echo.
echo =====================================================================
echo Building FinPlay Pro RELEASE APK...
echo =====================================================================
call gradlew.bat assembleRelease
if %ERRORLEVEL% equ 0 (
    echo.
    echo =====================================================================
    echo [SUCCESS] Release APK built successfully!
    echo Output location:
    echo %SCRIPT_DIR%app\build\outputs\apk\release
    echo =====================================================================
    explorer "%SCRIPT_DIR%app\build\outputs\apk\release"
) else (
    echo.
    echo [FAILED] Release build failed. Check signing configuration.
)
pause
goto exit_script

:clean_rebuild
echo.
echo Cleaning and rebuilding...
call gradlew.bat clean assembleDebug
pause
goto exit_script

:run_server
echo.
echo Launching local server at http://localhost:3000 ...
where node >nul 2>&1
if %ERRORLEVEL% equ 0 (
    node server.js
) else (
    echo Node.js is not installed or not in PATH.
    pause
)
goto exit_script

:open_studio
echo.
echo Opening in Android Studio...
if exist "%ProgramFiles%\Android\Android Studio\bin\studio64.exe" (
    start "" "%ProgramFiles%\Android\Android Studio\bin\studio64.exe" "%SCRIPT_DIR%"
) else (
    echo Android Studio standard path not found. Please open Android Studio manually and open:
    echo %SCRIPT_DIR%
    pause
)
goto exit_script

:open_output
if exist "%SCRIPT_DIR%app\build\outputs\apk" (
    explorer "%SCRIPT_DIR%app\build\outputs\apk"
) else (
    echo APK has not been built yet. Please run option 1 or 2 first.
    pause
)
goto exit_script

:exit_script
endlocal
