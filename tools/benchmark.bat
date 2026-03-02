@echo off
setlocal enabledelayedexpansion

:: Extract version from POM (same as start.bat/clicker.bat)
for /f "delims=" %%v in ('mvn help:evaluate -Dexpression^=project.version -q -DforceStdout 2^>nul ^| findstr /r "^[0-9]"') do set "PROJECT_VERSION=%%v"
if not defined PROJECT_VERSION (
    for /f "tokens=2 delims=<>" %%v in ('findstr /r "<version>[0-9]" pom.xml 2^>nul') do (
        if not defined PROJECT_VERSION set "PROJECT_VERSION=%%v"
    )
)
if not defined PROJECT_VERSION set "PROJECT_VERSION=unknown"

echo.
echo  ============================================
echo   ArtEvolver %PROJECT_VERSION% - Benchmark Runner
echo  ============================================
echo.

:: -------------------------------------------------------------------
:: Check prerequisites
:: -------------------------------------------------------------------
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo  [ERROR] Java not found on PATH.
    pause
    exit /b 1
)
echo  [OK] Java found

where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo  [ERROR] Maven not found on PATH.
    pause
    exit /b 1
)
echo  [OK] Maven found

:: -------------------------------------------------------------------
:: Select benchmark
:: -------------------------------------------------------------------
if "%1"=="" goto :menu

set "TEST_NAME=%1"
goto :run

:menu
echo.
echo  Select benchmark to run:
echo.
echo    1) Quick smoke test        (~12 seconds)
echo    2) Single-thread baseline  (~60 seconds)
echo    3) Thread scaling test     (~4 minutes)
echo    4) Population size test    (~3 minutes)
echo    5) All benchmarks
echo    0) Exit
echo.
set /p choice="  Enter choice [1-5]: "

if "%choice%"=="1" set "TEST_NAME=BenchmarkTest#quickBenchmark" & goto :run
if "%choice%"=="2" set "TEST_NAME=BenchmarkTest#singleThreadBaseline" & goto :run
if "%choice%"=="3" set "TEST_NAME=BenchmarkTest#threadScalingBenchmark" & goto :run
if "%choice%"=="4" set "TEST_NAME=BenchmarkTest#populationSizeBenchmark" & goto :run
if "%choice%"=="5" set "TEST_NAME=BenchmarkTest" & goto :run
if "%choice%"=="0" exit /b 0

echo  Invalid choice.
goto :menu

:: -------------------------------------------------------------------
:: Run benchmark
:: -------------------------------------------------------------------
:run
echo.
echo  Running: %TEST_NAME%
echo  Output will be saved to: artevolver-core\benchmarks\
echo.

call mvn test -pl artevolver-core "-Dtest=%TEST_NAME%"

echo.
echo  ============================================
echo  Benchmark complete. CSV files saved to:
echo    artevolver-core\benchmarks\
echo  ============================================
echo.

if "%1"=="" pause

endlocal
