#!/usr/bin/env bash

DIR=$(dirname "$0")
PATH=../../dist/bin:../../bin:$PATH
DEPS=$(dirname `type -p konanc`)/../dependencies

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

CLANG_linux=$DEPS/clang-llvm-3.9.0-linux-x86-64/bin/clang++
CLANG_macbook=$DEPS/clang-llvm-3.9.0-darwin-macos/bin/clang++

var=CLANG_${TARGET}
CLANG=${!var}

mkdir $DIR/build/
mkdir $DIR/build/clang/

$CLANG -std=c++11 -c $DIR/src/cpp/MessageChannel.cpp -o $DIR/build/clang/MessageChannel.bc -emit-llvm || exit 1