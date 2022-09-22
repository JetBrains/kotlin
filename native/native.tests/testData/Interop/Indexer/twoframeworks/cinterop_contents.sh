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

#!/bin/bash
# in case of no stdlib, build it within Kotlin project with
# ./gradlew ios_x64PlatformLibs

SCRIPT=`realpath $0`
FRAMEWORK_DIR=`dirname $SCRIPT`
#echo $FRAMEWORK_DIR

#Build a dependency klib
#echo "Building pod1"
cinterop -o pod1.klib -def pod1.def -compiler-option "-F$FRAMEWORK_DIR" || exit 1

# Build the dependent klib.
#echo "Building pod2"
#echo cinterop -o pod2.klib -def pod2.def -l ./pod1.klib -compiler-option "-F$FRAMEWORK_DIR" -compiler-option -fmodules
cinterop -o pod2.klib -def pod2.def -l ./pod1.klib -compiler-option "-F$FRAMEWORK_DIR" -compiler-option -fmodules -compiler-option -fmodules-cache-path=.

#cinterop -o pod2.klib -target ios_x64 -def pod2.def -l ./pod1.klib -compiler-option "-F." -compiler-option -fmodules
#echo klib contents ./pod1.klib
klib contents ./pod1.klib > pods.contents.txt
klib contents ./pod2.klib >> pods.contents.txt
grep -E " pod.Version" pods.contents.txt
