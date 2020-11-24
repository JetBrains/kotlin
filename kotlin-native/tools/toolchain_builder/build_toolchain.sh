#!/bin/bash

set -eou pipefail

TARGET=$1
VERSION=$2
HOME=/home/ct
ZLIB_VERSION=1.2.11

build_toolchain() {
  mkdir $HOME/build-$TARGET
  cd $HOME/build-$TARGET
  cp $HOME/toolchains/$TARGET/$VERSION.config .config
  ct-ng build
  cd ..
}

build_zlib() {
  TOOLCHAINS_PATH=$HOME/x-tools
  INSTALL_PATH=$TOOLCHAINS_PATH/$TARGET/$TARGET/sysroot/usr
  TOOLCHAIN_BIN_PREFIX=$TOOLCHAINS_PATH/$TARGET/bin/$TARGET
  cd $HOME/zlib-$ZLIB_VERSION

  CHOST=$TARGET \
  CC=$TOOLCHAIN_BIN_PREFIX-gcc \
  AR=$TOOLCHAIN_BIN_PREFIX-ar \
  RANLIB=$TOOLCHAIN_BIN_PREFIX-ranlib \
  ./configure \
  --prefix=$INSTALL_PATH

  make && make install
}

build_archive() {
  cd $HOME/x-tools
  FULL_NAME=$TARGET-$VERSION
  mv $TARGET $FULL_NAME
  ARCHIVE_NAME="$FULL_NAME.tar.xz"
  tar -czvf $ARCHIVE_NAME $FULL_NAME
  cp $ARCHIVE_NAME /artifacts/$ARCHIVE_NAME
}

echo "building toolchain for $TARGET"

build_toolchain
build_zlib
build_archive