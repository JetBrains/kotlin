#!/usr/bin/env bash

BOARD=stm32f4_disco
export ZEPHYR_BASE="PLEASE_SET_ZEPHYR_BASE"

if [ "$ZEPHYR_BASE" == "PLEASE_SET_ZEPHYR_BASE" ] ; then
    echo "Please set ZEPHYR_BASE in this build.sh."
    exit 1
fi

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-mac ;;
  linux*)   TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

GCC_ARM="$KONAN_DEPS/$TOOLCHAIN"

mkdir -p $DIR/build && cd $DIR/build

konanc $DIR/src/main.kt -target zephyr_$BOARD -linkerOpts -L$GCC_ARM/arm-none-eabi/lib/thumb -linkerOpts -lsupc++ -opt || exit 1

export ZEPHYR_GCC_VARIANT=gccarmemb
export GCCARMEMB_TOOLCHAIN_PATH=$GCC_ARM

[ -f CMakeCache.txt ] || cmake -DCMAKE_VERBOSE_MAKEFILE=ON -DBOARD=$BOARD .. || exit 1
make || exit 1

# make flash
#
# For our STM32 boards the OpenOCD unable to flash the binary,
# so we go with the following alternative utility:

echo 
echo "Now run 'make flash' to flash the .bin to the card."
echo
echo "Or, if that doesn't work, like, for example if you have an stm32f4-disco,"
echo "run the following command:"
echo "st-flash --reset write build/zephyr/zephyr.bin 0x08000000"
echo
