#!/bin/bash

root_dir=$(pwd)

# cache-redirector can't be used to upgrade lock files because NPM makes uncacheable API calls during the upgrade.
# We need to ensure the default registry is used during the upgrade so can be overridden later by a registry set in .npmrc
echo "Deleting .npmrc files..."
find . -name ".npmrc" -type f -delete

# Upgrade kotlin-js-store/yarn.lock
echo "Start upgrade 'kotlin-js-store/yarn.lock'"
rm -rf kotlin-js-store build/js && ./gradlew :kotlinUpgradeYarnLock -PcacheRedirectorEnabled=false
echo "End upgrade 'kotlin-js-store/yarn.lock'"

# Upgrade js/js.translator/testData/package-lock.json

echo "Start upgrade 'js/js.translator/testData/package-lock.json'"
cd ./js/js.translator/testData
npm upgrade

cd $root_dir
echo "End upgrade 'js/js.translator/testData/package-lock.json'"

# Upgrade libraries/kotlin.test/js/it/package-lock.json

echo "Start upgrade 'libraries/kotlin.test/js/it/package-lock.json'"
cd ./libraries/kotlin.test/js/it
npm upgrade

cd $root_dir
echo "End upgrade 'libraries/kotlin.test/js/it/package-lock.json'"

# Upgrade wasm/wasm.debug.browsers/package-lock.json

echo "Start upgrade 'wasm/wasm.debug.browsers/package-lock.json'"
cd ./wasm/wasm.debug.browsers
npm upgrade

cd $root_dir
echo "End upgrade 'wasm/wasm.debug.browsers/package-lock.json'"