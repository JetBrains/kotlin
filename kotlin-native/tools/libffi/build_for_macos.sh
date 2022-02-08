#!/bin/bash

set -eou pipefail

OUTPUT_PATH=${1?"Usage: $0 <output path>"}
if [ ! -d "$OUTPUT_PATH" ]; then
  echo "$(realpath "$OUTPUT_PATH") does not exists"
  exit 1
fi

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

# Apple Silicon-specific patch.
# Based on:
# * https://github.com/libffi/libffi/pull/565
# * https://github.com/libffi/libffi/pull/621
curl -LO https://raw.githubusercontent.com/Homebrew/formula-patches/06252df03c68aee70856e5842f85f20b259e5250/libffi/libffi-3.3-arm64.patch
patch -p1 --ignore-whitespace < "libffi-3.3-arm64.patch"

./configure --prefix="$(pwd)/$NAME" --disable-docs --enable-shared=no
make install
tar -czf "$OUTPUT_PATH"/$NAME.tar.gz "$NAME"
popd
popd