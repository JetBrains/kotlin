# Calculator sample

This example shows how to use Kotlin common module (located in [arithmeticParser](arithmeticParser/)) in different environments.
Currently for
* Android (see [androidApp](androidApp/))
* iOS (see [iosApp](iosApp/))
* plain JVM (cli) (see [cliApp](cliApp/))

## Common

Common Kotlin module contains arithmetic expressions parser.

## Android App
The common module may be used in an Android application.

Please make sure that Android SDK version 28 is installed, using Android SDK manager in Android Studio.
See https://developer.android.com/studio/index.html for more details on Android Studio or
`$ANDROID_HOME/tools/bin/sdkmanager "platforms;android-28" "build-tools;28.0.3"` from command line.

To build use `ANDROID_HOME=<your path to android sdk> ../gradlew assemble`.

Run `$ANDROID_HOME/platform-tools/adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`
to deploy the apk on the Android device or emulator.

Note: If you are importing project to IDEA for the first time, you might need to put `local.properties` file
with the following content:

    sdk.dir=<your path to Android SDK>

## iOS
The iOS project compiles Kotlin module to a framework (see [iosApp](iosApp/)). The framework can be easily included in an existing iOS project (e.g. written in Swift or Objective-C)

To build and run the iOS sample do the following:

1.  Open `iosApp/calculator.xcodeproj` with Xcode.
2.  Open the project's target through project navigator, go to tab 'General'.
    In 'Identity' section change the bundle ID to the unique string in
    reverse-DNS format. Then select the team in 'Signing' section.
    
    See the
    [Xcode documentation](https://developer.apple.com/library/content/documentation/IDEs/Conceptual/AppDistributionGuide/ConfiguringYourApp/ConfiguringYourApp.html#//apple_ref/doc/uid/TP40012582-CH28-SW2)
    for more info.
3.  Now build and run the application with Xcode.

The iOS application is written in Swift. It uses Kotlin module as a library.
Kotlin module is built into Objective-C framework by invoking Gradle
from custom "Run Script" build phase, and this framework is imported into
the Xcode project.

## Plain JVM
The common module can also be used in JVM application built by Kotlin/JVM compiler with Gradle.
To build and run it, go to [cliApp](cliApp/) directory and use
```
../gradlew runProgram
```
