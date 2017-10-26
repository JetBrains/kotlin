#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )
PATH=$DIR/../../dist/bin:$DIR/../../bin:$PATH

CFLAGS_macbook="-I/opt/local/include -I/usr/local/include"
CFLAGS_linux=-I/usr/include

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=CFLAGS_${TARGET}
CFLAGS=${!var}
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

mkdir -p $DIR/build/c_interop/
mkdir -p $DIR/build/bin/

cinterop -compilerOpts "$CFLAGS" -def $DIR/src/main/c_interop/libgit2.def -target $TARGET \
	 -o $DIR/build/c_interop/libgit2 || exit 1

konanc -target $TARGET $DIR/src/main/kotlin -library $DIR/build/c_interop/libgit2 \
       -o $DIR/build/bin/GitChurn || exit 1

echo "Artifact path is $DIR/build/bin/GitChurn.kexe"
