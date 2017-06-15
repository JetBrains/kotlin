@echo off

setlocal

set DIR=.
set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw

call cinterop -def "%DIR%\src\main\c_interop\stdio.def" -target "%TARGET%" -o stdio
if ERRORLEVEL 1 exit /b %ERRORLEVEL%

call konanc -target "%TARGET%" "%DIR%\src\main\kotlin\CsvParser.kt" -library stdio -o CsvParser
exit /b %ERRORLEVEL%
