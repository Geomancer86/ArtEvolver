@echo off
setlocal enabledelayedexpansion

echo.
echo  ============================================
echo   ArtEvolver v3.1 - Launcher
echo  ============================================
echo.

:: -------------------------------------------------------------------
:: Check Java
:: -------------------------------------------------------------------
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo  [ERROR] Java not found on PATH.
    echo          Install Java 17+ from https://adoptium.net/
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
:: Build (skip if already compiled)
:: -------------------------------------------------------------------
if not exist "artevolver-core\target\classes\com\rndmodgames\evolver\ArtEvolver.class" (
    echo.
    echo  Building ArtEvolver...
    echo.
    call mvn compile -pl artevolver-core -q
    if %errorlevel% neq 0 (
        echo.
        echo  [ERROR] Build failed. Trying full rebuild...
        call mvn clean compile -pl artevolver-core
        if %errorlevel% neq 0 (
            echo  [ERROR] Build failed. Check errors above.
            pause
            exit /b 1
        )
    )
    echo  [OK] Build complete
) else (
    echo  [OK] Build exists (use --rebuild to force)
)

:: Handle --rebuild flag
if "%1"=="--rebuild" (
    echo.
    echo  Forcing rebuild...
    call mvn clean compile -pl artevolver-core -q
    echo  [OK] Rebuild complete
)

:: -------------------------------------------------------------------
:: Resolve classpath and run
:: -------------------------------------------------------------------
echo.
echo  Starting ArtEvolver...
echo  (Close the window or press Ctrl+C to stop)
echo.

:: Build classpath from target/classes + all dependency jars
set "CP=artevolver-core\target\classes"

:: Add Maven dependency jars if they exist
if exist "artevolver-core\target\dependency" (
    for %%j in (artevolver-core\target\dependency\*.jar) do (
        set "CP=!CP!;%%j"
    )
)

:: Also include the local Maven repository jars via mvn exec:java
:: This is the most reliable approach since dependencies aren't copied locally
call mvn exec:java -pl artevolver-core -Dexec.mainClass="com.rndmodgames.evolver.ArtEvolver" -q

if %errorlevel% neq 0 (
    echo.
    echo  Note: If the above failed, the exec-maven-plugin may need to resolve
    echo        dependencies first. Running with full output:
    echo.
    call mvn exec:java -pl artevolver-core -Dexec.mainClass="com.rndmodgames.evolver.ArtEvolver"
)

endlocal
