#! /bin/bash

set -e
git clone https://github.com/JetBrains-Research/litmuskt new-version -b development
cd new-version
git apply ../repo-integration.patch
echo applied patch
rm -rf ../core && mv core ..
rm -rf ../testsuite && mv testsuite ..
echo overwritten subprojects
cd ..
rm -rf new-version
echo cleaned up
echo update complete
