#!/bin/sh
# exit when any command fails
set -e
KONANHOME=$1
FULLTESTPATH=$2
WORKDIR=$3
FMODULES_ARG="$4 $5"

TESTDATA=$FULLTESTPATH/../..
KLIB=klib.klib
cd $WORKDIR
$KONANHOME/bin/cinterop -o $KLIB -def $FULLTESTPATH/pod1.def -compiler-option -F$TESTDATA $FMODULES_ARG
$KONANHOME/bin/klib contents $KLIB
exit 0
