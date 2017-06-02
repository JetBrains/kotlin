@echo off
setlocal
set DIR=.
set "PATH=..\..\dist\bin;..\..\bin;%PATH%"
if "%TARGET%" == "" set TARGET=mingw

set CFLAGS=-I\msys64\mingw64\include\SDL2
set LFLAGS=-L\msys64\mingw64\lib -lSDL2

call cinterop -def "%DIR%\sdl.def" -compilerOpts "%CFLAGS%" -target "%TARGET%" -o sdl || exit /b
call konanc -target "%TARGET%" "%DIR%\Tetris.kt" -library sdl -linkerOpts "%LFLAGS%" -opt -o Tetris || exit /b

copy Tetris.kexe Tetris.exe
copy \msys64\mingw64\bin\SDL2.dll SDL2.dll
