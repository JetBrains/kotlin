#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

KOTLIN_DIR="$DIR/kotlin-library"
IOS_DIR="$DIR/ios-app"

#Check CocoaPods version.
REQUIRED_POD_VERSION="1.6.1"
POD_VERSION=`pod --version`
EARLIER_VERSION=`echo "$POD_VERSION $REQUIRED_POD_VERSION" | tr " " "\n" | sort -V | head -1`

if [ "$EARLIER_VERSION" != "$REQUIRED_POD_VERSION" ]; then
    echo "ERROR: This version of CocoaPods is unsupported. Current version is $POD_VERSION. Minimal required version is $REQUIRED_POD_VERSION."
    echo "See update instructions at https://guides.cocoapods.org/using/getting-started.html#updating-cocoapods"
    exit 1
fi

# Prepare Kotlin/Native project to be consumed by CocoaPods.
"$KOTLIN_DIR/gradlew" -p "$KOTLIN_DIR" podspec

# Run CocoaPods to configure the Xcode project.
pod --project-directory="$IOS_DIR" install

# Run Xcode to build the app.
xcodebuild -sdk iphonesimulator -arch x86_64 -configuration Release -workspace "$IOS_DIR/ios-app.xcworkspace" -scheme ios-app
