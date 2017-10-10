# Android Native Activity
 This example shows how to build an Android Native Activity.

The example will render a textured dodecahedron using OpenGL ES library. It can be rotated with fingers.
Please make sure that Android SDK version 25 is installed, using Android SDK manager in Android Studio.
See https://developer.android.com/studio/index.html for more details on Android Studio or
`$ANDROID_HOME/tools/bin/sdkmanager "platforms;android-25" "build-tools;25.0.2"` from command line.

To build use `ANDROID_HOME=<your path to android sdk> ../gradlew buildApk`.

Run `$ANDROID_HOME/sdk/platform-tools/adb install -r build/outputs/apk/androidNativeActivity-arm-debug.apk`
to deploy the apk on the Android device or emulator (note that only ARM-based devices are currently supported).
