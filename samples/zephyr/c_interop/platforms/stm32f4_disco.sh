# Copyright 2010-2017 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This is board specific.

if [ x"$BOARD" == x ]; then
    echo "This script is to be included by build.sh"
    exit 0
fi

# TODO: we need a robust automated way to ask Zephyr for 
# all these $ZEPHYR_BASE based paths and the proper defines.
# TODO: The D flag in `-copt -Dxxx` sequence is interpreted to be a jvm property,
# so we workaround the using -Xclang.

cinterop -def $DIR/c_interop/platforms/$BOARD.def \
        -pkg platform.zephyr.$BOARD \
        -copt '-Xclang -DSTM32F407xx' \
        -copt -I$ZEPHYR_BASE/kernel/include \
        -copt -I$ZEPHYR_BASE/arch/arm/include \
        -copt -I$ZEPHYR_BASE/arch/arm/soc/st_stm32/stm32f4 \
        -copt -I$ZEPHYR_BASE/arch/arm/soc/st_stm32/stm32f4/include \
        -copt -I$ZEPHYR_BASE/arch/arm/soc/st_stm32/include \
        -copt -I$ZEPHYR_BASE/boards/arm/$BOARD \
        -copt -I$ZEPHYR_BASE/include \
        -copt -I$ZEPHYR_BASE/include/drivers \
        -copt -I$ZEPHYR_BASE/ext/hal/cmsis/Include \
        -copt -I$ZEPHYR_BASE/ext/hal/st/stm32cube/stm32f4xx/soc \
        -copt -I$ZEPHYR_BASE/ext/hal/st/stm32cube/stm32f4xx/drivers/include \
        -copt -I$ZEPHYR_BASE/ext/hal/st/stm32cube/stm32f4xx/drivers/include/Legacy \
        -copt -I$ZEPHYR_BASE/drivers \
        -copt -I$DIR/build/zephyr/include/generated \
        -copt -I$DIR/build/zephyr/include/generated/syscalls \
        -o $DIR/c_interop/platforms/build/$BOARD \
        -target zephyr_$BOARD || exit 1
