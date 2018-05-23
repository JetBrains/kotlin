@echo off
setlocal
set DIR=.
if "%KONAN_DATA_DIR%"=="" (set "KONAN_DATA_DIR=%userprofile%\.konan")
set "PATH=..\..\dist\bin;..\..\bin;%KONAN_DATA_DIR%\dependencies\msys2-mingw-w64-x86_64-gcc-7.2.0-clang-llvm-5.0.0-windows-x86-64\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw
rem Requires default mingw64 install path yet.
set MINGW=\msys64\mingw64

set "CFLAGS=-I%MINGW%\include\SDL2"
rem Add -Wl,--subsystem,windows for making GUI subsystem application.
set "LFLAGS=%DIR%\Tetris.res -L%MINGW%\lib -lSDL2"

call cinterop -def "%DIR%\src\main\c_interop\sdl.def" -compilerOpts "%CFLAGS%" -target "%TARGET%" -o sdl || exit /b
rem Windows build requires Windows Resource Compiler in paths.
call windres "%DIR%\Tetris.rc" -O coff -o "%DIR%\Tetris.res" || exit /b
call konanc -target "%TARGET%" "%DIR%\src\main\kotlin\Tetris.kt" -library sdl -linkerOpts "%LFLAGS%" -opt -o Tetris || exit /b

copy %MINGW%\bin\SDL2.dll SDL2.dll
copy src\main\resources\tetris_all.bmp tetris_all.bmp
