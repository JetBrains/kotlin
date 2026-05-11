@echo off

setlocal
set _BIN_DIR=%~dp0
set _KOTLIN_HOME=%_BIN_DIR%..

if "%JAVA_HOME%"=="" (
  echo error: JAVA_HOME is not set; kotlinc-native-image requires JAVA_HOME environment variable 1>&2
  exit /b 1
)

"%_BIN_DIR%kotlinc-native-image.exe" ^
  "-Djava.home=%JAVA_HOME%" ^
  "-Dkotlin.home=%_KOTLIN_HOME%" ^
  %*
