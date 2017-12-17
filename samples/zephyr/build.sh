#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source "$DIR/../konan.sh"

mkdir -p $DIR/build && cd $DIR/build

konanc $DIR/src/main.kt -target zephyr

DEP="$HOME/.konan/dependencies"
export ZEPHYR_BASE="$DEP/zephyr-zephyr-v1.10.0"
export ZEPHYR_GCC_VARIANT=gccarmemb
export GCCARMEMB_TOOLCHAIN_PATH="$DEP/target-gcc-toolchain-6-2017q2-linux-armemb/"
export PATH="$DEP/zephyr-host-tools-1-linux/bin:$PATH"

[ -f CMakeCache.txt ] || cmake -DBOARD=qemu_cortex_m3 ..
make run
