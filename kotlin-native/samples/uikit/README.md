# iOS UIKit sample

This example shows how to write iOS UI applications in Kotlin, and run them on
Apple devices, such as an iPhone.

To build and run the sample do the following:

0.  Optional: install Kotlin Xcode plugin: https://github.com/touchlab/xcode-kotlin to have
    syntax highlighting and better debugging support.

1.  Open  `UIKitSample.xcodeproj` with Xcode, set development team to your own
   and make bundle ID unique in project settings.
  or

1a.  Similarly modify `bundleIdPrefix` and `DEVELOPMENT_TEAM` in `project.yml` and
    then generate Xcode project with `xcodegen` (https://github.com/yonaskolb/XcodeGen/).

2.  Now build and run the application with Xcode on a connected iPhone  or simulator.

Note that in this example we do not use storyboards, and instead create user interface
components programmatically. Defining UI with storyboards in pure Kotlin iOS applications
is supported as well.


