#! /bin/bash

git clone https://github.com/JetBrains-Research/litmuskt new-version -b development
cd new-version
git apply ../repo-integration.patch
mv -f core ../core
mv -f testsuite ../testsuite
cd ..
rm new-version  # should be empty by now
