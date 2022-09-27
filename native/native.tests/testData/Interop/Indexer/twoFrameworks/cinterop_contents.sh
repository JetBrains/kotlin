#!/bin/sh
# exit when any command fails
set -e
KONANHOME=$1
FULLTESTPATH=$2
WORKDIR=$3
FMODULES_ARG="$4 $5"

TESTDATA=$FULLTESTPATH/../..
POD1_KLIB=pod1.klib
POD2_KLIB=pod2.klib
KLIBS_CONTENTS=klibs.contents.txt

cd $WORKDIR
$KONANHOME/bin/cinterop -o $POD1_KLIB -def $FULLTESTPATH/pod1.def -compiler-option -F$TESTDATA $FMODULES_ARG
$KONANHOME/bin/cinterop -o $POD2_KLIB -l $POD1_KLIB -def $FULLTESTPATH/pod2.def -compiler-option -F$TESTDATA $FMODULES_ARG

$KONANHOME/bin/klib contents $POD1_KLIB > $KLIBS_CONTENTS
$KONANHOME/bin/klib contents $POD2_KLIB >> $KLIBS_CONTENTS
grep -E "package pod| pod.Version" $KLIBS_CONTENTS

exit 0
