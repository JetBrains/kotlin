@echo off
rem based on scalac.bat from the Scala distribution
rem ##########################################################################
rem # Copyright 2002-2011, LAMP/EPFL
rem # Copyright 2011-2017, JetBrains
rem #
rem # This is free software; see the distribution for copying conditions.
rem # There is NO warranty; not even for MERCHANTABILITY or FITNESS FOR A
rem # PARTICULAR PURPOSE.
rem ##########################################################################

setlocal enabledelayedexpansion
call :set_home

set "TOOL_NAME=%1"
shift

if "%_TOOL_CLASS%"=="" set _TOOL_CLASS=org.jetbrains.kotlin.cli.utilities.MainKt

if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%_JAVACMD%"=="" set _JAVACMD=java

set JAVA_ARGS=
set KONAN_ARGS=

:again
set "ARG=%1"
if not "!ARG!" == "" (
    if "!ARG:~0,2!" == "-D" (
        set "JAVA_ARGS=%JAVA_ARGS% %ARG%"
        goto next
    )
    if "!ARG:~0,2!" == "-J" (
        set "JAVA_ARGS=%JAVA_ARGS% !ARG:~2!"
        goto next
    )
    if "!ARG!" == "--time" (
        set "KONAN_ARGS=%KONAN_ARGS% --time"
        set "JAVA_ARGS=%JAVA_ARGS% -Dkonan.profile=true"
        goto next
    )

    set "KONAN_ARGS=%KONAN_ARGS% %ARG%"

    :next
    shift
    goto again
)

call :set_java_version

if !_java_major_version! geq 24 (
  rem Allow JNI access for all compiler code. In particular, this is needed for jansi (see `PlainTextMessageRenderer`).
  set JAVA_OPTS=!JAVA_OPTS! "--enable-native-access=ALL-UNNAMED"

  rem Suppress unsafe deprecation warnings, see KT-76799 and IDEA-370928.
  set JAVA_OPTS=!JAVA_OPTS! "--sun-misc-unsafe-memory-access=allow"
)

set "KONAN_LIB=%_KONAN_HOME%\konan\lib"

set "KONAN_JAR=%KONAN_LIB%\kotlin-native-compiler-embeddable.jar"

set "KONAN_CLASSPATH=%KONAN_JAR%"
set JAVA_OPTS=-ea ^
    -Xmx3G ^
    -XX:TieredStopAtLevel=1 ^
    -Dfile.encoding=UTF-8 ^
    -Dkonan.home="%_KONAN_HOME%" ^
    %JAVA_OPTS%

set LIBCLANG_DISABLE_CRASH_RECOVERY=1

"%_JAVACMD%" %JAVA_OPTS% %JAVA_ARGS% -cp "%KONAN_CLASSPATH%" %_TOOL_CLASS% %TOOL_NAME% %KONAN_ARGS%

exit /b %ERRORLEVEL%
goto end

rem ##########################################################################
rem # subroutines

:set_home
  set _BIN_DIR=
  for %%i in (%~sf0) do set _BIN_DIR=%_BIN_DIR%%%~dpsi
  set _KONAN_HOME=%_BIN_DIR%..
goto :eof
rem
rem Parses "java -version" output and stores the major version to _java_major_version.
rem Note that this only loads the first component of the version, so "1.8.0_265" -> "1".
rem But it's fine because major version is 9 for JDK 9, and so on.
rem Needs to be executed in the EnableDelayedExpansion mode.
:set_java_version
  set _version=
  rem Parse output and take the third token from the string containing " version ".
  rem It should be something like "1.8.0_275" or "15.0.1".
  for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i " version "') do (
    rem Split the string by "-" or "." and take the first token.
    for /f "delims=-. tokens=1" %%j in ("%%i") do (
      rem At this point, _version should be something like "1 or "15. Note the leading quote.
      set _version=%%j
    )
  )
  if "!_version!"=="" (
    rem If failed to parse the output, set the version to 1.
    set _java_major_version=1
  ) else (
    rem Strip the leading quote.
    set _java_major_version=!_version:~1!
  )
goto :eof

:end
endlocal
