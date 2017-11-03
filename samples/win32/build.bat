@echo off
setlocal
set DIR=.

if defined KONAN_HOME (
    set "PATH=%KONAN_HOME%\bin;%PATH%"
) else (
    set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
)

if "%TARGET%" == "" set TARGET=mingw

set "LFLAGS=-Wl,--subsystem,windows"

call konanc -target "%TARGET%" "%DIR%\src\main\kotlin\MessageBox.kt" -linkerOpts "%LFLAGS%" -opt -o MessageBox || exit /b
