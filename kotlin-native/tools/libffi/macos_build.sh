#!/bin/bash
set -eou pipefail

OUTPUT_PATH=$1
PATCH="$(pwd)/patches/libffi-3.3-aarch64-homebrew.patch"

if [ -z "$(sysctl -q hw.optional.arm64)" ]
then
  ARCH=x86_64
else
  ARCH=arm64
fi

VERSION=1
NAME=libffi-3.3-$VERSION-macos-$ARCH

pushd "$(mktemp -d -t libffi)"
curl -LO https://github.com/libffi/libffi/releases/download/v3.3/libffi-3.3.tar.gz
tar -xf libffi-3.3.tar.gz
pushd libffi-3.3
patch -p1 --ignore-whitespace < "$PATCH"
./configure --prefix="$(pwd)/$NAME" --disable-docs --enable-shared=no
make install
tar -czf "$OUTPUT_PATH"/$NAME.tar.gz "$NAME"
popd
popd


