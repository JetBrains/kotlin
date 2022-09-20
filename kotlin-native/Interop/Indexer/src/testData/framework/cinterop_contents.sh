#!/bin/sh
# exit when any command fails
set -e
cinterop "$@"
# Arg indices below must correspond to parameter building in AbstractIndexerFromSourcesTest.doTestSuccessfulCInterop
KLIB=$2
DEF=$4
KLIB_OUTPUT=$KLIB.contents.txt
klib contents $KLIB > $KLIB_OUTPUT
diff $DEF.contents.gold.txt $KLIB_OUTPUT
exit 0
