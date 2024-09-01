#!/bin/sh
set -x #echo on

jarPath=$1
dir1=$2
dir2=$3
reportDirPath=$4

mkdir -p $reportDir

java -jar $jarPath dir $dir1 $dir2 $reportDir > $reportDir/comparator.log 2>&1
