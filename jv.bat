@echo off
REM JValue CLI launcher — auto-compiles if cli\classes is missing
REM Usage: jv <command> [args]

set JAVA_HOME=D:\Downloads\jdk-25.0.4_windows-x64_bin\jdk-25.0.4
set PATH=%JAVA_HOME%\bin;%PATH%
set CP=build\classes;cli\classes

REM Auto-build JValue library if not yet compiled
if not exist "build\classes\com\jvalue\Json.class" (
    echo [jv] Building JValue library...
    call build.bat build
)

REM Auto-compile CLI if not yet compiled
if not exist "cli\classes\JValueCli.class" (
    echo [jv] Compiling CLI...
    if not exist "cli\classes" mkdir cli\classes
    javac --release 25 -cp "build\classes" -d "cli\classes" "cli\JValueCli.java"
    if errorlevel 1 (
        echo [jv] CLI compile FAILED.
        exit /b 1
    )
    echo [jv] CLI ready.
)

java -cp "%CP%" JValueCli %*
