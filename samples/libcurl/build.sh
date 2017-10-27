#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../dist/bin:$DIR/../../bin:$PATH

CFLAGS_macbook=-I/opt/local/include
CFLAGS_linux="-I /usr/include -I /usr/include/x86_64-linux-gnu"

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

cinterop -compilerOpts "$CFLAGS" -compilerOpts -I$DIR -def $DIR/src/main/c_interop/libcurl.def -target $TARGET \
	 -o $DIR/build/c_interop/libcurl || exit 1

konanc -target $TARGET $DIR/src/main/kotlin -library $DIR/build/c_interop/libcurl \
       -o $DIR/build/bin/Curl || exit 1

echo "Artifact path is $DIR/build/bin/Curl.kexe"
