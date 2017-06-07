@echo off
setlocal
set DIR=.
set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw

set "LFLAGS=-Wl,--subsystem,windows"

call cinterop -def "%DIR%\win32.def" -target "%TARGET%" -o win32 || exit /b
call konanc -target "%TARGET%" "%DIR%\MessageBox.kt" -library win32 -linkerOpts "%LFLAGS%" -opt -o MessageBox || exit /b

copy MessageBox.kexe MessageBox.exe
