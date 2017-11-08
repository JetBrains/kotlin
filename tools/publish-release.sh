#!/bin/bash

# Release publisher. Downloads the working tree in the working directory (it must be empty),
# builds bundle and gradle-plugin, check them and upload to bintray/CDN.

# Usage: publish-release.sh [--no-upload] <version>
#   --no-upload - don't upload the built binaries (gradle plugin and bundle)
#                 to bintray/CDN, only test them.

# The script uses the following environment variables:
# BINTRAY_USER and BINTRAY_KEY to upload gradle plugin to bintray;
# CDN_USER and CDN_PASS to upload the bundle to CDN.

function stage {
    echo
    echo "==================================="
    date
    echo "$1"
}

set -eu

if [[ $# -lt 1 ]]; then
    echo "No version is set. Usage: `basename $0` [--no-upload] [--no-build] <version>"
    exit 1
fi

UPLOAD=true
BUILD=true
# Prepare command line args.
while [[ $# -gt 1 ]]; do
    arg="$1"
    case $arg in
        --no-upload)
            UPLOAD=false
            ;;
        --no-build)
            BUILD=false
            ;;
        *)
            echo "Unknown option: $arg. Usage: `basename $0` [--no-upload] <version>"
            exit 1
            ;;
    esac
    shift
done

# The variables used.
case "$OSTYPE" in
  darwin*)  OS=macos ;;
  linux*)   OS=linux ;;
  *)        echo "unknown: $OSTYPE" && exit 1;;
esac

VERSION=${1}
BRANCH="v${VERSION}-fixes"
REPO="https://github.com/JetBrains/kotlin-native.git"
TREE_DIR=${PWD}
BUNDLE_TAR="$TREE_DIR/kotlin-native-$OS-$VERSION.tar.gz"
BUNDLE_DIR="$TREE_DIR/kotlin-native-$OS-$VERSION"
WAIT_TIME=120

if [ "$BUILD" == "true" ]; then
    # Build and test.
    # 1. Download kotlin-native sources
    stage "Cloning repo: $REPO (branch: $BRANCH)"
    git clone "$REPO" .
    git checkout "$BRANCH"

    # 2.1 Update dependencies
    stage "Building bundle: $BUNDLE_TAR"
    ./gradlew dependencies:update

    # 2.2 Build the bundle
    ./gradlew clean bundle
    tar -xf "$BUNDLE_TAR"
fi

# 3. Check the bundle in commandline mode
stage "Checking commandline build"
cd "$BUNDLE_DIR/samples"
./build.sh

# 4. Build the gradle plugin
stage "Building gradle plugin"
cd "$TREE_DIR"
./gradlew tools:kotlin-native-gradle-plugin:jar

# 5. Build samples in the tree with the plugin built
stage "Building samples with the plugin built"
cd "$TREE_DIR/samples"
./gradlew build --refresh-dependencies

if [ "$UPLOAD" == "true" ]; then
    # 6. Upload the plugin
    stage "Uploading the gradle plugin to bintray"
    cd "$TREE_DIR"
    export BINTRAY_USER
    export BINTRAY_KEY
    ./gradlew :tools:kotlin-native-gradle-plugin:bintrayUpload -Poverride

    sleep 10 # Wait some time to ensure that the plugin can be downloaded on the following step.

    # 5. Build the bundle samples with the plugin
    stage "Building samples with gradle plugin and bundle compiler"
    cd "$BUNDLE_DIR/samples"
    ./gradlew build --refresh-dependencies

    # 6. Upload the bundle to the CDN
    stage "Uploading the bundle to CDN: $BUNDLE_TAR -> ftp://uploadcds.labs.intellij.net/builds/releases/$VERSION/$OS"
    curl --upload-file "$BUNDLE_TAR" "ftp://$CDN_USER:$CDN_PASS@uploadcds.labs.intellij.net/builds/releases/$VERSION/$OS"
    echo "Available at https://download.jetbrains.com/kotlin/native/builds/releases/$VERSION/$OS/$BUNDLE_TAR"

    echo "Wait ${WAIT_TIME} seconds to ensure that the bundle can be downloaded."
    sleep ${WAIT_TIME}

    # 7. Remove gradle.properties and build the bundle samples with gradle plugin.
    stage "Building samples with gradle plugin and downloaded compiler"
    cd "$BUNDLE_DIR/samples"
    rm -rf gradle.properties
    ./gradlew build --refresh-dependencies
fi

date
echo "Done."
