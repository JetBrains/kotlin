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

cinterop -def $DIR/src/main/c_interop/opengl.def -target $TARGET \
	 -o $DIR/build/c_interop/opengl || exit 1

konanc -target $TARGET $DIR/src/main/kotlin/OpenGlTeapot.kt -library $DIR/build/c_interop/opengl \
       -o $DIR/build/bin/OpenGlTeapot || exit 1

echo "Artifact path is $DIR/build/bin/OpenGlTeapot.kexe"
