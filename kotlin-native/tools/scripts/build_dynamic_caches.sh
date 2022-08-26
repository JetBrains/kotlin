#!/bin/bash
set -e
#
# Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

#
# This script builds dynamic caches for a given target.
# It is useful to run to check for regressions after Xcode update.
#

TARGET=$1
KONAN_HOME=../../dist

echo Building dynamic cache for $TARGET...
$KONAN_HOME/bin/generate-platform -t $TARGET -k dynamic_cache -c $KONAN_HOME/klib/cache/$TARGET-gDYNAMIC
echo Done.

