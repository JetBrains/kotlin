# Calculator sample

This example shows how to use Kotlin library (located in [parser/src](parser/src/)) in different environments.
Currently for
* iOS (see [calculator](calculator/))
* plain JVM (cli) (see [jvmCliApp](jvmCliApp/))
* Android (see [androidApp](androidApp/))

## iOS
The iOS project compile the library to a framework. The framework can easily included in an existing iOS project (e.g. written in Swift or Objective-C)

To build and run the iOS sample do the following:

1.  Open `samples/calculator/calculator.xcodeproj` with Xcode.
2.  Open the project's target through project navigator, go to tab 'General'.
    In 'Identity' section change the bundle ID to the unique string in
    reverse-DNS format. Then select the team in 'Signing' section.
    
    See the
    [Xcode documentation](https://developer.apple.com/library/content/documentation/IDEs/Conceptual/AppDistributionGuide/ConfiguringYourApp/ConfiguringYourApp.html#//apple_ref/doc/uid/TP40012582-CH28-SW2)
    for more info.
3.  Now build and run the application with Xcode.

The sample consists of:

1.  Xcode iOS application project, written in Swift. It uses Kotlin library to
    parse simple arithmetic expressions.
2.  Kotlin library source code and build script. It is built into Objective-C
    framework by invoking Gradle from custom "Run Script" build phase, and this
    framework is imported into the Xcode project.

## Using the same code with Kotlin/JVM
The library can also be compiled to a `.jar` by Kotlin/JVM compiler with Gradle. 
Just run from the [library dir](parser/): 
```
../../gradlew jar
```
This will generate an `arithmetic-parser.jar` in `parser/build/libs/`.

## Plain JVM
The plain jvm sample (written in Kotlin/JVM) will use the library as a gradle dependency.

To build and run it, go to [jvmCliApp](jvmCliApp/) directory and use
```
../../gradlew run
```

To build the distribution:
```
../../gradlew distZip
```
(the result will be available as
`jvmCliApp/build/distributions/KotlinCalculator.zip`)

## Android App
The Android App uses - similar to the plain JVM sample - the library as a gradle dependency.

To build and run the Android sample do the following:

1.  Open `samples/calculator/androidApp` in Android Studio
2.  Now build and run the androidApp application
