#!/bin/sh
# exit when any command fails
set -e
TESTPATH=$1
WORK_DIR=$2
FMODULES_ARG="$3 $4"

TESTDATA=$TESTPATH/../..
POD1_KLIB=pod1.klib
POD2_KLIB=pod2.klib
KLIBS_CONTENTS=klibs.contents.txt

cd $WORK_DIR
cinterop -o $POD1_KLIB -def $TESTPATH/pod1.def -compiler-option -F$TESTDATA $FMODULES_ARG
cinterop -o $POD2_KLIB -l $POD1_KLIB -def $TESTPATH/pod2.def -compiler-option -F$TESTDATA $FMODULES_ARG

klib contents $POD1_KLIB > $KLIBS_CONTENTS
klib contents $POD2_KLIB >> $KLIBS_CONTENTS
grep -E "package pod| pod.Version" $KLIBS_CONTENTS

exit 0
