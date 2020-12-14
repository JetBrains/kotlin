#!/bin/bash
set -eou pipefail

INSTALL_PATH=$1
QEMU_VERSION=5.1.0
DEPENDENCY_VERSION=2

function build_qemu() {
  mkdir build
  ./configure \
      --prefix="$PWD/build" \
      --static \
      --disable-debug-info \
      --disable-werror \
      --disable-system \
      --enable-linux-user \

  make -j"$(nproc)"
  make install
}

function build_archives() {
  cd build/bin
  for f in qemu-* ; do
    DEPENDENCY_NAME="$f-static-$QEMU_VERSION-linux-$DEPENDENCY_VERSION"
    mkdir -p $DEPENDENCY_NAME
    mv $f $DEPENDENCY_NAME/$f
    tar -czvf $INSTALL_PATH/"$DEPENDENCY_NAME.tar.gz" "$DEPENDENCY_NAME"
  done
}

build_qemu
build_archives