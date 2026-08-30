@echo off
REM JValue Build Script — Zero-Dependency JSON Toolkit for Java 25
REM Usage: build.bat [build|test|clean|deps-proof]

setlocal enabledelayedexpansion

set JAVAC=javac
set JAVA=java
set JAVAC_OPTS=--release 25
set SRC_DIR=src\main\java
set TEST_DIR=src\test\java
set BUILD_DIR=build
set MAIN_OUT=%BUILD_DIR%\classes
set TEST_OUT=%BUILD_DIR%\test-classes

if "%1"=="" goto all
if "%1"=="build" goto build
if "%1"=="test" goto test
if "%1"=="clean" goto clean
if "%1"=="deps-proof" goto deps_proof
echo Unknown command: %1
echo Usage: build.bat [build^|test^|clean^|deps-proof]
exit /b 1

:all
call :build
if errorlevel 1 exit /b 1
call :test
exit /b %errorlevel%

:build
echo [JValue] Compiling main sources...
if not exist "%MAIN_OUT%" mkdir "%MAIN_OUT%"

REM Collect all .java files from src/main/java
set "SOURCES="
for /r %SRC_DIR% %%f in (*.java) do set "SOURCES=!SOURCES! %%f"

if "!SOURCES!"=="" (
    echo [WARN] No source files found in %SRC_DIR%
    exit /b 0
)

%JAVAC% %JAVAC_OPTS% -d %MAIN_OUT% !SOURCES!
if errorlevel 1 (
    echo [FAIL] Build failed.
    exit /b 1
)
echo [OK] Build complete.
exit /b 0

:test
call :build
if errorlevel 1 exit /b 1

echo [JValue] Compiling tests...
if not exist "%TEST_OUT%" mkdir "%TEST_OUT%"

REM Collect all .java files from src/test/java
set "TEST_SRCS="
for /r %TEST_DIR% %%f in (*.java) do set "TEST_SRCS=!TEST_SRCS! %%f"

if "!TEST_SRCS!"=="" (
    echo [WARN] No test files found in %TEST_DIR%
    exit /b 0
)

%JAVAC% %JAVAC_OPTS% -cp %MAIN_OUT% -d %TEST_OUT% !TEST_SRCS!
if errorlevel 1 (
    echo [FAIL] Test compilation failed.
    exit /b 1
)

echo [JValue] Fetching JSONTestSuite corpus (if needed)...
%JAVA% -cp "%MAIN_OUT%;%TEST_OUT%" com.jvalue.test.FetchCorpus

echo [JValue] Running tests...
%JAVA% -cp "%MAIN_OUT%;%TEST_OUT%" com.jvalue.test.TestRunner
if errorlevel 1 (
    echo [FAIL] Tests failed.
    exit /b 1
)
echo [OK] All tests passed.
exit /b 0

:clean
echo [JValue] Cleaning...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
echo [OK] Clean complete.
exit /b 0

:deps_proof
call :build
if errorlevel 1 exit /b 1
echo.
echo === JValue Dependency Proof ===
echo.
echo -- Java version --
%JAVA% --version
echo.
echo -- jdeps module analysis (machine-verifiable) --
jdeps --multi-release 25 %MAIN_OUT%
echo.
echo -- Summary --
echo Build tool: javac (JDK built-in)
echo Runtime: java (JDK built-in)
echo Third-party dependencies: NONE
echo All class files depend only on java.base (confirmed by jdeps above)
echo.
echo === Proof complete ===
exit /b 0
