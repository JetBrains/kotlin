::
:: Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
:: that can be found in the license/LICENSE.txt file.
::

:: This is board specific.

if "%BOARD%" == "" (
    echo This script is to be included by build.bat
    exit /b
)

:: TODO: we need a robust automated way to ask Zephyr for
:: all these %ZEPHYR_BASE% based paths and the proper defines.
:: TODO: The D flag in `-compilerOpts -Dxxx` sequence is interpreted to be a jvm property,
:: so we workaround the using -Xclang.

call cinterop -def %DIR%/c_interop/platforms/%BOARD%.def ^
        -pkg platform.zephyr.%BOARD% ^
        -compilerOpts "-Xclang -DSTM32F407xx" ^
        -compilerOpts -I%ZEPHYR_BASE%/kernel/include ^
        -compilerOpts -I%ZEPHYR_BASE%/arch/arm/include ^
        -compilerOpts -I%ZEPHYR_BASE%/soc/arm/st_stm32/stm32f4 ^
        -compilerOpts -I%ZEPHYR_BASE%/soc/arm/st_stm32/common ^
        -compilerOpts -I%ZEPHYR_BASE%/boards/arm/%BOARD% ^
        -compilerOpts -I%ZEPHYR_BASE%/include ^
        -compilerOpts -I%ZEPHYR_BASE%/include/drivers ^
        -compilerOpts -I%ZEPHYR_BASE%/ext/hal/cmsis/Include ^
        -compilerOpts -I%ZEPHYR_BASE%/ext/hal/st/stm32cube/stm32f4xx/soc ^
        -compilerOpts -I%ZEPHYR_BASE%/ext/hal/st/stm32cube/stm32f4xx/drivers/include ^
        -compilerOpts -I%ZEPHYR_BASE%/ext/hal/st/stm32cube/stm32f4xx/drivers/include/Legacy ^
        -compilerOpts -I%ZEPHYR_BASE%/drivers ^
        -compilerOpts -I%DIR%/build/zephyr/include/generated ^
        -compilerOpts -I%DIR%/build/zephyr/include/generated/syscalls ^
        -o %DIR%/c_interop/platforms/build/%BOARD% ^
        -target zephyr_%BOARD% || exit /b
