# iOS UIKit sample

This example shows how to write iOS UI applications in Kotlin, and run them on
Apple devices, such as an iPhone.

To build and run the sample do the following:

1.  Open `samples/uikit/konan.xcodeproj` with Xcode.

2.  Open the project's target through project navigator, go to tab 'General'.
    In 'Identity' section change the bundle ID to the unique string in
    reverse-DNS format. Then select the team in 'Signing' section.

    See the
    [Xcode documentation](https://developer.apple.com/library/content/documentation/IDEs/Conceptual/AppDistributionGuide/ConfiguringYourApp/ConfiguringYourApp.html#//apple_ref/doc/uid/TP40012582-CH28-SW2)
    for more info.

3.  Now build and run the application on a connected iPhone with Xcode.
    (The compilation will be slow for the first time).

The sample consists of the Xcode project and Kotlin source code. Xcode project
contains the UI built with Interface Builder and headers for Objective-C classes,
which are implemented in Kotlin.

During build the executable compiled from Objective-C sources is replaced with
the one compiled from Kotlin sources.
