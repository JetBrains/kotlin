# Calculator sample

This example shows how to use Kotlin common module (located in [common/](common/)) in different environments.
Currently for
* Android (see [android](android/))
* iOS (see [ios/calculator](ios/calculator/))
* plain JVM (cli) (see [jvm](jvm/))

## Common

Common Kotlin module contains arithmetic expressions parser.

## Android App
The common module may be used in an Android application.

To build and run the Android sample do the following:

1.  Open the project in Android Studio 3.1
2.  Create a new Android App configuration. Choose module `android`.
3.  Now build and run the configuration created.

## iOS
The iOS project compiles Kotlin module to a framework (see [ios](ios/)). The framework can be easily included in an existing iOS project (e.g. written in Swift or Objective-C)

To build and run the iOS sample do the following:

1.  Open `ios/calculator.xcodeproj` with Xcode.
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
To build and run it, go to [jvm](jvm/) directory and use
```
../gradlew run
```

To build the distribution:
```
../gradlew distZip
```
(the result will be available as
`jvm/build/distributions/KotlinCalculator.zip`)
