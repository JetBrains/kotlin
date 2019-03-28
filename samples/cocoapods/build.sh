#!/usr/bin/env bash

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

KOTLIN_DIR="$DIR/kotlin-library"
IOS_DIR="$DIR/ios-app"

# Prepare Kotlin/Native project to be consumed by CocoaPods.
"$KOTLIN_DIR/gradlew" -p "$KOTLIN_DIR" podspec

# Run CocoaPods to configure the Xcode project.
pod --project-directory="$IOS_DIR" install

# Run Xcode to build the app.
xcodebuild -sdk iphonesimulator12.1 -configuration Release -workspace "$IOS_DIR/ios-app.xcworkspace" -scheme ios-app

