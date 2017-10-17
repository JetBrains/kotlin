#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../dist/bin:$DIR/../../bin:$PATH
DEPS=$(dirname `type -p konanc`)/../dependencies

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

# Ensure that C compiler for the target is available.
konanc --check_dependencies -target $TARGET || exit 1

CLANG_linux=$DEPS/clang-llvm-3.9.0-linux-x86-64/bin/clang++
CLANG_macbook=$DEPS/clang-llvm-3.9.0-darwin-macos/bin/clang++

var=CLANG_${TARGET}
CLANG=${!var}

mkdir -p $DIR/build/clang/

$CLANG -H -std=c++11 -c $DIR/src/main/cpp/MessageChannel.cpp -o $DIR/build/clang/MessageChannel.bc -emit-llvm || exit 1
