@echo off
setlocal enabledelayedexpansion

:: Extract version from POM (filter to lines starting with a digit to skip any Maven error output)
for /f "delims=" %%v in ('mvn help:evaluate -Dexpression^=project.version -q -DforceStdout 2^>nul ^| findstr /r "^[0-9]"') do set "PROJECT_VERSION=%%v"
if not defined PROJECT_VERSION (
    for /f "tokens=2 delims=<>" %%v in ('findstr /r "<version>[0-9]" pom.xml 2^>nul') do (
        if not defined PROJECT_VERSION set "PROJECT_VERSION=%%v"
    )
)
if not defined PROJECT_VERSION set "PROJECT_VERSION=unknown"

echo.
echo  ============================================
echo   ArtEvolver %PROJECT_VERSION% - Evolution Clicker
echo  ============================================
echo.
echo  Browser-based clicker game. No Java UI needed.
echo  Drop an image in the browser and start evolving!
echo.

:: -------------------------------------------------------------------
:: Check Java
:: -------------------------------------------------------------------
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo  [ERROR] Java not found on PATH.
    echo          Install Java 21+ from https://adoptium.net/
    echo.
    pause
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER=%%~v"
)
echo  [OK] Java found: %JAVA_VER%

:: -------------------------------------------------------------------
:: Check Maven
:: -------------------------------------------------------------------
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo  [ERROR] Maven not found on PATH.
    echo          Install Maven 3.6+ from https://maven.apache.org/
    echo.
    pause
    exit /b 1
)
echo  [OK] Maven found

:: -------------------------------------------------------------------
:: Build (skip if already compiled for this version)
:: -------------------------------------------------------------------
set "NEED_BUILD=0"
if not exist "artevolver-core\target\classes\com\rndmodgames\evolver\ArtEvolver.class" set "NEED_BUILD=1"
if exist "artevolver-core\target\.build-version" (
    set /p "CACHED_VERSION=" <"artevolver-core\target\.build-version"
) else (
    set "CACHED_VERSION="
)
if not "!CACHED_VERSION!"=="%PROJECT_VERSION%" set "NEED_BUILD=1"

if "!NEED_BUILD!"=="1" (
    echo.
    echo  Building ArtEvolver...
    echo.
    call mvn compile -pl artevolver-core -q
    if !errorlevel! neq 0 (
        echo.
        echo  [ERROR] Build failed. Trying full rebuild...
        call mvn clean compile -pl artevolver-core
        if !errorlevel! neq 0 (
            echo  [ERROR] Build failed. Check errors above.
            pause
            exit /b 1
        )
    )
    echo %PROJECT_VERSION%> "artevolver-core\target\.build-version"
    echo  [OK] Build complete
) else (
    echo  [OK] Build exists
)

:: -------------------------------------------------------------------
:: Launch clicker mode
:: -------------------------------------------------------------------
echo.
echo  Starting Evolution Clicker...
echo  The game will open in your browser automatically.
echo  (Press Ctrl+C to stop the server)
echo.

call mvn exec:java -pl artevolver-core -Dexec.mainClass="com.rndmodgames.evolver.ArtEvolver" -Dexec.args="--clicker" -q

endlocal
