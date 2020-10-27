#!/usr/bin/env bash

KONAN_TOOLCHAIN_VERSION=1
TARBALL_zephyr_arm=target-sysroot-$KONAN_TOOLCHAIN_VERSION-zephyr-arm
OUT=`pwd`

if [ -z "ZEPHYR_SDK_INSTALL_DIR" ]; then
    echo "Using default Zephyr SDK install location"
    export ZEPHYR_SDK_INSTALL_DIR="/opt/zephyr-sdk"
fi

sdk=armv5-zephyr-eabi
p=$ZEPHYR_SDK_INSTALL_DIR/sysroots/$sdk/usr
echo "Packing SDK $sdk as $OUT/$TARBALL_zephyr_arm.tar.gz..."
tar -czvf $OUT/$TARBALL_zephyr_arm.tar.gz -C $p .
