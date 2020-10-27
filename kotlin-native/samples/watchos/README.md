# watchOS sample

This example shows how to write watchOS UI applications in Kotlin, and run them on
Apple Watch or simulator.

To build and run the sample do the following:

0.  Optional: install Kotlin Xcode plugin: https://github.com/touchlab/xcode-kotlin to have
    syntax highlighting and better debugging support.

1.  Open `watchosSample.xcodeproj` set development team to your own and make bundle ID unique
    in project setting.
   or

1a.  Similarly modify `bundleIdPrefix`, `DEVELOPMENT_TEAM` and `WKAppBundleIdentifier` in `project.yml`
    and generate Xcode project with `xcodegen` (https://github.com/yonaskolb/XcodeGen/).
    Open generated `watchosSample.xcodeproj` with Xcode.

2. Update property `WKAppBundleIdentifier` in `plists/Ext/Info.plist` with new ID of the watch application,
   if not regenerating project.

3.  Now build and run the application on a connected iPhone with paired Apple Watch or simulator.

Note that in this example we do not use storyboards, and instead create user interface
components programmatically.

First run of application on the physical watch could be blocked, so run it from watch menu
and explicitly confirm that developer is trusted.
