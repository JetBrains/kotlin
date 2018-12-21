#!/usr/bin/env bash

BOARD=stm32f4_disco
export ZEPHYR_BASE="PLEASE_SET_ZEPHYR_BASE"

if [ "$ZEPHYR_BASE" == "PLEASE_SET_ZEPHYR_BASE" ] ; then
    echo "Please set ZEPHYR_BASE in this build.sh."
    exit 1
fi

export KONAN_DATA_DIR=$HOME/.konan
export KONAN_DEPS=$KONAN_DATA_DIR/dependencies

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

if [ -z "$KONAN_HOME" ]; then
    PATH="$DIR/../../dist/bin:$DIR/../../bin:$PATH"
else
    PATH="$KONAN_HOME/bin:$PATH"
fi

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-mac ;;
  linux*)   TOOLCHAIN=gcc-arm-none-eabi-7-2017-q4-major-linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

GNU_ARM="$KONAN_DEPS/$TOOLCHAIN"

rm -rf $DIR/build || exit 1
mkdir -p $DIR/build && cd $DIR/build

export ZEPHYR_TOOLCHAIN_VARIANT=gnuarmemb
export GNUARMEMB_TOOLCHAIN_PATH=$GNU_ARM

[ -f CMakeCache.txt ] || cmake -DCMAKE_VERBOSE_MAKEFILE=ON -DBOARD=$BOARD .. || exit 1

# We need generated headers to be consumed by `cinterop`,
# so we preconfigure the project with `make zephyr`.
make zephyr

. $DIR/c_interop/platforms/$BOARD.sh

rm -f program.o

mkdir -p $DIR/build/kotlin

konanc $DIR/src/main.kt \
        -target zephyr_$BOARD \
        -r $DIR/c_interop/platforms/build \
        -l $BOARD \
        -opt -g -o $DIR/build/kotlin/program || exit 1

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
