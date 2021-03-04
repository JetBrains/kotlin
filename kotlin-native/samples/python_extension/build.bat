setlocal
set DIR=.

if defined KONAN_HOME (
    set "PATH=%KONAN_HOME%\bin;%PATH%"
) else (
    set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
)
kotlinc-native -p dynamic src/main/kotlin/Server.kt -o server

rem Prepare MSVC build environment, and .lib file for linking with our .dll.
SET VS90COMNTOOLS=%VS140COMNTOOLS%
\Program Files (x86)\Microsoft Visual Studio 14.0\VC\bin\lib.exe" /def:server.def /out:server.lib  /machine:X64

python src/main/python/setup.py install
