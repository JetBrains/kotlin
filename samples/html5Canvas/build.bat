@echo off
setlocal enableextensions

set DIR=%~dp0

if defined KONAN_HOME (
    set "PATH=%KONAN_HOME%\bin;%PATH%"
) else (
    set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
)

if not exist build\bin\ (mkdir build\bin)
if not exist build\klib\ (mkdir build\klib)

::: TODO: use the proper path and names
call jsinterop -pkg kotlinx.interop.wasm.dom ^
        -o %DIR%\build\klib\dom -target wasm32 || exit /b

call konanc %DIR%\src\main\kotlin ^
        -r %DIR%\build\klib -l dom ^
        -o %DIR%\build\bin\html5Canvas -target wasm32 || exit 1

echo "Artifact path is %DIR%\build\bin\html5Canvas.wasm"
echo "Artifact path is %DIR%\build\bin\html5Canvas.wasm.js"
echo "Check out %DIR%\index.html"
echo "Serve %DIR%\ by an http server for the demo"
::: For example run: py -m http.server 8080
