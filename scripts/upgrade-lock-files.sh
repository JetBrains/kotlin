#!/bin/bash

root_dir=$(pwd)

# Upgrade kotlin-js-store/yarn.lock
echo "Start upgrade 'kotlin-js-store/yarn.lock'"
rm -rf kotlin-js-store && ./gradlew :kotlinUpgradeYarnLock
echo "End upgrade 'kotlin-js-store/yarn.lock'"

# Upgrade kotlin-native/tools/performance-server/kotlin-js-store/yarn.lock

echo "Start upgrade 'kotlin-native/tools/performance-server/kotlin-js-store/yarn.lock'"
cd ./kotlin-native/tools/performance-server/
rm -rf kotlin-js-store && ./gradlew :kotlinUpgradeYarnLock

cd $root_dir
echo "End upgrade 'kotlin-native/tools/performance-server/kotlin-js-store/yarn.lock'"

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