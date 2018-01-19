# iOS calculator sample

This example shows how to use Kotlin library compiled to framework from
an existing iOS project (e.g. written in Swift or Objective-C).

To build and run the sample do the following:

1.  Open `samples/calculator/calculator.xcodeproj` with Xcode.
2.  Open the project's target through project navigator, go to tab 'General'.
    In 'Identity' section change the bundle ID to the unique string in
    reverse-DNS format. Then select the team in 'Signing' section.
    
    See the
    [Xcode documentation](https://developer.apple.com/library/content/documentation/IDEs/Conceptual/AppDistributionGuide/ConfiguringYourApp/ConfiguringYourApp.html#//apple_ref/doc/uid/TP40012582-CH28-SW2)
    for more info.
3.  Now build and run the application on a connected iPhone with Xcode.

The sample consists of:

1.  Xcode iOS application project, written in Swift. It uses Kotlin library to
    parse simple arithmetic expressions.
2.  Kotlin library source code and build script. It is built into Objective-C
    framework by invoking Gradle from custom "Run Script" build phase, and this
    framework is imported into the Xcode project.

## Using the same code with Kotlin/JVM

The library can also be compiled to a `.jar` by Kotlin/JVM compiler with Gradle. 
Just run from the [sample root dir](../): 
```
./gradlew calculator:jar
```
This will generate a `calculator.jar` in `build/libs/`.

There is also simple Kotlin/JVM CLI app available in `jvmCliApp` subdirectory.
To build and run it, use
```
./gradlew calculator:jvmCliApp:run
```

To build the distribution:
```
./gradlew calculator:jvmCliApp:distZip
```
(the result will be available as
`jvmCliApp/build/distributions/KotlinCalculator.zip`)
