#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../dist/bin:$DIR/../../bin:$PATH

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

mkdir -p $DIR/build/c_interop/
mkdir -p $DIR/build/bin/

cinterop -def $DIR/src/main/c_interop/sockets.def -copt "$CFLAGS" -target $TARGET \
	 -o $DIR/build/c_interop/sockets || exit 1

konanc $COMPILER_ARGS -target $TARGET $DIR/src/main/kotlin/EchoServer.kt \
       -library $DIR/build/c_interop/sockets \
       -o $DIR/build/bin/EchoServer || exit 1

echo "Artifact path is $DIR/build/bin/EchoServer.kexe"
