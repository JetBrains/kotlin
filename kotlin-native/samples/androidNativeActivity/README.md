# Android Native Activity

This example shows how to build an Android Native Activity. Also, we provide an example
bridging mechanism for the Java APIs, callable from Native side.

The example will render a textured dodecahedron using OpenGL ES library. It can be rotated with fingers.
Please make sure that Android SDK version 28 is installed, using Android SDK manager in Android Studio.
See https://developer.android.com/studio/index.html for more details on Android Studio or
`$ANDROID_HOME/tools/bin/sdkmanager "platforms;android-28" "build-tools;28.0.3"` from command line.
We use JniBridge to call vibration service on the Java side for short tremble on startup.

To build use `ANDROID_HOME=<your path to android sdk> ../gradlew assemble`.

Run `$ANDROID_HOME/platform-tools/adb install -r build/outputs/apk/debug/androidNativeActivity-debug.apk`
to deploy the apk on the Android device or emulator.

Note that "Emulated Performance - Graphics" in AVD manager must be set to "Software - GLES 2.0".

Note: If you are importing project to IDEA for the first time, you might need to put `local.properties` file
with the following content:

    sdk.dir=<your path to Android SDK>
