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

call konanc %DIR%\src\jsinterop\kotlin ^
        -includeBinary %DIR%\src\jsinterop\js\jsinterop.js ^
        -p library -o %DIR%\build\klib\jsinterop -target wasm32 || exit /b

call konanc %DIR%\src\stubGenerator\kotlin ^
        -e org.jetbrains.kotlin.konan.jsinterop.tool.main ^
        -o %DIR%\build\bin\generator || exit /b

::: TODO: make a couple of args to name the result, for example.
%DIR%\build\bin\generator.exe || exit /b

::: TODO: use the proper path and names
call konanc kotlin_stubs.kt ^
        -includeBinary js_stubs.js ^
        -l %DIR%\build\klib\jsinterop ^
        -p library -o %DIR%\build\klib\canvas -target wasm32 || exit /b

call konanc %DIR%\src\main\kotlin ^
        -r %DIR%\build\klib -l jsinterop -l canvas ^
        -o %DIR%\build\bin\html5Canvas -target wasm32 || exit 1

echo "Artifact path is %DIR%\build\bin\html5Canvas.wasm"
echo "Artifact path is %DIR%\build\bin\html5Canvas.wasm.js"
echo "Check out %DIR%\index.html"
echo "Serve %DIR%\ by an http server for the demo"
::: For example run: py -m http.server 8080
