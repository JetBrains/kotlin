#!/bin/sh
# exit when any command fails
set -e
TESTPATH=$1
WORK_DIR=$2
FMODULES_ARG=$3

TESTDATA=$TESTPATH/../..
KLIB=klib.klib
cd $WORK_DIR
cinterop -o $KLIB -def $TESTPATH/pod1.def -compiler-option -I$TESTDATA/include $FMODULES_ARG
klib contents $KLIB
exit 0
