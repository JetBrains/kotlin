@echo off

setlocal

set DIR=.
set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw

call konanc -target "%TARGET%" "%DIR%\src\main\kotlin\CsvParser.kt" -o CsvParser
exit /b %ERRORLEVEL%
