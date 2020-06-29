Prototype Apple Gradle Plugin
-----------------------------

### How to build

In `kotlin-ultimate` run:
`./gradlew :kotlin-ultimate:libraries:tools:apple-gradle-plugin:publish`

Then link output repo to desired path:
`ln -s kotlin-ultimate/libraries/tools/apple-gradle-plugin/build/repo $MY_REPO_PATH`

### How to use

Add to `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("$MY_REPO_PATH")
        gradlePluginPortal()
    }
}
```

Add to `build.gradle.kts`:

```kotlin
buildscript {
    repositories {
        // use `snapshots` instead of `releases` when EAP/SNAPSHOT version was used for `versions.intellijSdk`
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://repo.labs.intellij.net/intellij-proprietary-modules")
        maven("https://jetbrains.bintray.com/intellij-third-party-dependencies/")
    }
}

plugins {
    id("org.jetbrains.gradle.apple.applePlugin") version "0.1-SNAPSHOT-192.6262.58"
}

apple {
    iosApp {
        launchStoryboard = "LaunchScreen"
        mainStoryboard = "Main"
    }
}

dependencies {
    "iosAppImplementation"(ios.binaries.getFramework(NativeBuildType.DEBUG).linkTask.outputs.files)
}
```

Add sources to project:

```
src
└── iosAppMain
    └── apple
        ├── AppDelegate.swift
        ├── Assets.xcassets
        ├── LaunchScreen.storyboard
        ├── Main.storyboard
        └── ViewController.swift
```