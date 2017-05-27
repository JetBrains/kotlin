#!/usr/bin/env bash

DIR=.
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

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.
var=CLANG_${TARGET}
CLANG=${!var}

$CLANG -std=c++11 -c $DIR/MessageChannel.cpp -o $DIR/MessageChannel.bc -emit-llvm || exit 1
cinterop -def $DIR/MessageChannel.def -copt "-I$DIR" -target $TARGET -o $DIR/MessageChannel.kt.bc || exit 1
konanc $DIR/Concurrent.kt -library $DIR/MessageChannel.kt.bc \
       -nativelibrary $DIR/MessageChannel.bc -o Concurrent.kexe || exit 1
