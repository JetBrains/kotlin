#!/bin/bash

root_dir=$(pwd)

NPM_REGISTRY="https://cache-redirector.jetbrains.com/registry.npmjs.org"

# cache-redirector.jetbrains.com can't be used to upgrade lock files because NPM makes uncacheable API calls during the upgrade.
# We need to ensure the default registry is used during the upgrade. The default registry can be overridden later by a registry set in .npmrc
./gradlew cleanNpmRc

# Upgrade kotlin-js-store/yarn.lock
echo "Start upgrade 'kotlin-js-store/yarn.lock'"
rm -rf kotlin-js-store && ./gradlew :kotlinUpgradeYarnLock -PcacheRedirectorEnabled=false

# yarn 1.x doesn't and won't support overriding registry used in yarn.lock, so we need to replace it manually
# https://github.com/yarnpkg/yarn/issues/6436#issuecomment-426728911
sed -i -e "s#https://registry.yarnpkg.com#${NPM_REGISTRY}#g" kotlin-js-store/yarn.lock

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