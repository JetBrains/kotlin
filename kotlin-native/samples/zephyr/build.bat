@echo off
setlocal

set BOARD=stm32f4_disco
set ZEPHYR_BASE=%userprofile%\zephyr
set TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-win32

set DIR=%~dp0
if "%KONAN_DATA_DIR%"=="" (set KONAN_DATA_DIR=%userprofile%\.konan)
set KONAN_DEPS=%KONAN_DATA_DIR%/dependencies
set GNU_ARM=%KONAN_DEPS%/%TOOLCHAIN%
set ZEPHYR_TOOLCHAIN_VARIANT=gnuarmemb
set GNUARMEMB_TOOLCHAIN_PATH=%GNU_ARM%

if defined KONAN_HOME (
    set "PATH=%KONAN_HOME%\bin;%PATH%"
) else (
    set "PATH=%DIR%..\..\dist\bin;%DIR%..\..\bin;%PATH%"
)

if not exist build\ (mkdir build)
cd build

if not exist CMakeCache.txt (cmake -GNinja -DBOARD=%BOARD% .. || exit /b)

:: We need generated headers to be consumed by `cinterop`,
:: so we preconfigure the project with `make zephyr`.
ninja zephyr

call %DIR%\c_interop\platforms\%BOARD%.bat

del program.o

mkdir %DIR%\build\kotlin

call konanc %DIR%/src/main.kt ^
        -target zephyr_%BOARD% ^
        -r %DIR%/c_interop/platforms/build ^
        -l %BOARD% ^
        -opt -g -o %DIR%/build/kotlin/program || exit /b

ninja || exit /b

:: ninja flash
::
:: For our STM32 boards the OpenOCD unable to flash the binary,
:: so we go with the following alternative utility:

echo.
echo Now run 'ninja flash' to flash the .bin to the card.
echo.
echo Or, if that doesn't work, like, for example if you have an stm32f4-disco,
echo run the following command:
echo st-flash --reset write build/zephyr/zephyr.bin 0x08000000
echo.
