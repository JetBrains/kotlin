# Android Native Activity
 This example shows how to build an Android Native Activity.

The example will render a textured dodecahedron using OpenGL ES library. It can be rotated with fingers.
Please make sure that Android SDK version 25 is installed, using Android SDK manager.

To build use `ANDROID_HOME=<your path to android sdk> ../gradlew buildApk`.

Run `adb install build/outputs/apk/androidNativeActivity-arm-debug.apk` to deploy the apk on Android device or emulator
(note that only ARM-based devices are currently supported).
