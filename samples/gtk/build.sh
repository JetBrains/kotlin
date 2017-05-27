#!/usr/bin/env bash

PATH=../../dist/bin:../../bin:$PATH
DIR=.

IPREFIX_macbook=-I/opt/local/include
#IPREFIX_macbook=-I/usr/local/include
IPREFIX_linux=-I/usr/include
LINKER_ARGS_macbook="-L/opt/local/lib -lglib-2.0 -lgdk-3.0 -lgtk-3 -lgio-2.0 -lgobject-2.0"
LINKER_ARGS_linux="-L/usr/lib/x86_64-linux-gnu -lglib-2.0 -lgdk-3 -lgtk-3 -lgio-2.0 -lgobject-2.0"

if [ x$TARGET == x ]; then
case "$OSTYPE" in
  darwin*)  TARGET=macbook ;;
  linux*)   TARGET=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac
fi

var=IPREFIX_${TARGET}
IPREFIX="${!var}"
var=LINKER_ARGS_${TARGET}
LINKER_ARGS=${!var}
var=COMPILER_ARGS_${TARGET}
COMPILER_ARGS=${!var} # add -opt for an optimized build.

if [ ! -f $DIR/gtk3.bc ]; then
  echo "Generating GTK stubs (once), may take few mins depending on the hardware..."
  cinterop -J-Xmx8g -copt $IPREFIX/atk-1.0 -copt $IPREFIX/gdk-pixbuf-2.0 -copt $IPREFIX/cairo -copt $IPREFIX/pango-1.0 \
  -copt -I/opt/local/lib/glib-2.0/include -copt -I/usr/lib/x86_64-linux-gnu/glib-2.0/include -copt -I/usr/local/lib/glib-2.0/include \
  -copt $IPREFIX/gtk-3.0 -copt $IPREFIX/glib-2.0 -def $DIR/gtk3.def \
  -target $TARGET -o $DIR/gtk3.bc || exit 1
fi
konanc -target $TARGET $DIR/src -library $DIR/gtk3.bc -linkerArgs "$LINKER_ARGS" -o $DIR/Gtk3Demo.kexe || exit 1


