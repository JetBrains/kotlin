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
:: all these $ZEPHYR_BASE based paths and the proper defines.
:: TODO: The D flag in `-copt -Dxxx` sequence is interpreted to be a jvm property,
:: so we workaround the using -Xclang.

call cinterop -def %DIR%/c_interop/platforms/%BOARD%.def ^
        -pkg platform.zephyr.%BOARD% ^
        -copt "-Xclang -DSTM32F407xx" ^
        -copt -I%ZEPHYR_BASE%/kernel/include ^
        -copt -I%ZEPHYR_BASE%/arch/arm/include ^
        -copt -I%ZEPHYR_BASE%/arch/arm/soc/st_stm32/stm32f4 ^
        -copt -I%ZEPHYR_BASE%/arch/arm/soc/st_stm32/stm32f4/include ^
        -copt -I%ZEPHYR_BASE%/arch/arm/soc/st_stm32/include ^
        -copt -I%ZEPHYR_BASE%/boards/arm/%BOARD% ^
        -copt -I%ZEPHYR_BASE%/include ^
        -copt -I%ZEPHYR_BASE%/include/drivers ^
        -copt -I%ZEPHYR_BASE%/ext/hal/cmsis/Include ^
        -copt -I%ZEPHYR_BASE%/ext/hal/st/stm32cube/stm32f4xx/soc ^
        -copt -I%ZEPHYR_BASE%/ext/hal/st/stm32cube/stm32f4xx/drivers/include ^
        -copt -I%ZEPHYR_BASE%/ext/hal/st/stm32cube/stm32f4xx/drivers/include/Legacy ^
        -copt -I%ZEPHYR_BASE%/drivers ^
        -copt -I%DIR%/build/zephyr/include/generated ^
        -copt -I%DIR%/build/zephyr/include/generated/syscalls ^
        -o %DIR%/c_interop/platforms/build/%BOARD% ^
        -target zephyr_%BOARD% || exit /b
