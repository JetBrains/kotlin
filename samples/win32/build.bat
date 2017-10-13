@echo off
setlocal
set DIR=.
set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw

set "LFLAGS=-Wl,--subsystem,windows"

call konanc -target "%TARGET%" "%DIR%\src\main\kotlin\MessageBox.kt" -linkerOpts "%LFLAGS%" -opt -o MessageBox || exit /b
