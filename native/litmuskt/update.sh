#! /bin/bash

set -e
git clone https://github.com/JetBrains-Research/litmuskt new-version -b development
cd new-version

# `git apply` does not delete files if they were changed, so we do it manually
git rm -qrf *gradle* *jcstress* codegen/ cli/ README.md
git rm -q testsuite/src/commonMain/kotlin/org/jetbrains/litmuskt/LitmusTestExtensions.kt
git rm -q testsuite/src/commonMain/kotlin/org/jetbrains/litmuskt/generated/LitmusTestRegistry.kt
git rm -q testsuite/src/nativeMain/kotlin/org/jetbrains/litmuskt/tests/WordTearingNative.kt
# and then we can apply the patch
git apply ../repo-integration.patch

# ../repo-integration.patch
echo applied patch
rm -rf ../core && mv core ..
rm -rf ../testsuite && mv testsuite ..
echo overwritten subprojects
cd ..
rm -rf new-version
echo cleaned up
echo update complete
