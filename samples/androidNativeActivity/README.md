# Android Native Activity
 This example shows how to build an Android Native Activity. Also, we provide an example
bridging mechanism for the Java APIs, callable from Native side.

The example will render a textured dodecahedron using OpenGL ES library. It can be rotated with fingers.
Please make sure that Android SDK version 25 is installed, using Android SDK manager in Android Studio.
See https://developer.android.com/studio/index.html for more details on Android Studio or
`$ANDROID_HOME/tools/bin/sdkmanager "platforms;android-25" "build-tools;25.0.2"` from command line.
We use JniBridge to call vibration service on the Java side for short tremble on startup.

To build use `ANDROID_HOME=<your path to android sdk> ../gradlew build`.

Run `$ANDROID_HOME/platform-tools/adb install -r build/outputs/apk/debug/androidNativeActivity-debug.apk`
to deploy the apk on the Android device or emulator (note that only ARM-based devices are currently supported).

