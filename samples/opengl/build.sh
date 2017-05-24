#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.

LINKER_ARGS_macbook="-framework OpenGL -framework GLUT"
LINKER_ARGS_linux="-L/usr/lib/x86_64-linux-gnu -lglut -lGL -lGLU"

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

cinterop -def $DIR/opengl.def -target $TARGET -o opengl || exit 1
konanc -target $TARGET $DIR/OpenGlTeapot.kt -library opengl -linkerArgs "$LINKER_ARGS" -o OpenGlTeapot || exit 1
