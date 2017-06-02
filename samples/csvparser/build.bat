@echo off

setlocal

set DIR=.
set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw

call cinterop -def "%DIR%\stdio.def" -target "%TARGET%" -o stdio
if ERRORLEVEL 1 exit /b %ERRORLEVEL%

call konanc -target "%TARGET%" "%DIR%\CsvParser.kt" -library stdio -o CsvParser
exit /b %ERRORLEVEL%