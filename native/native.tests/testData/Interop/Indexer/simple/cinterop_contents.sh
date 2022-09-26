#!/bin/sh
# exit when any command fails
set -e
HOME=$1
TESTPATH=$2
WORK_DIR=$3
FMODULES_ARG="$4 $5"

KONANHOME=$HOME/kotlin-native/dist
FULLTESTPATH=$HOME/$TESTPATH
TESTDATA=$FULLTESTPATH/../..
KLIB=klib.klib
cd $WORK_DIR
$KONANHOME/bin/cinterop -o $KLIB -def $FULLTESTPATH/pod1.def -compiler-option -I$TESTDATA/include $FMODULES_ARG
$KONANHOME/bin/klib contents $KLIB
exit 0
