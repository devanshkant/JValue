@echo off
REM JValue CLI launcher
REM Usage: jv <command> [args]
REM Example: jv help
REM          jv pretty demo\data\users.json
REM          jv get demo\data\users.json /users/0/name

set JAVA_HOME=D:\Downloads\jdk-25.0.4_windows-x64_bin\jdk-25.0.4
set PATH=%JAVA_HOME%\bin;%PATH%
set CP=build\classes;cli\classes

java -cp "%CP%" JValueCli %*
