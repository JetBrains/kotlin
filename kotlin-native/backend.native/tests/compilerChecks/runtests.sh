#
# Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

for TEST in kotlin-native/backend.native/tests/compilerChecks/*.kt; do
  echo "$TEST";
  konanc -p library -Xsuppress-version-warnings "$TEST" "$@";
  retVal=$?
  if [ $retVal -eq 0 ]; then
    echo "  MISSING EXPECTED ERROR FOR $TEST"
  fi
done
