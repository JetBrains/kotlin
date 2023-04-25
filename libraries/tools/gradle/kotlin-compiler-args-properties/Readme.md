## Description

Contains a plugin for Gradle tests to set Kotlin arguments (api and language version) via 'gradle.properties'
or via `-P` command line flag.

### Available properties

- `kotlin.test.languageVersion` - sets `-language-version` to the given value
- `kotlin.test.apiVersion` - sets '-api-version' to the given value
- `kotlin.test.overrideUserValue` - make the best effort to override values set explicitly by the user in the build script

### Plugin usage in the external projects

Plugin is also possible to use not only in the tests, but on the external projects. 
To do this follow these steps:
- Run `./gradlew :gradle:kotlin-compiler-args-properties:install` (short variant `./gradlew :gr:k-c-a-p:i`)
- In the external project modify `settings.gradle.kts` following way:
```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        // other repos defined by the project
    }
    
    plugins {
        id("org.jetbrains.kotlin.test.kotlin-compiler-args-properties") version "1.9.255-SNAPSHOT"
        // other plugin declarations by the project
    }
}
```
- In the external project modify root `build.gradle.kts` following way:
```kotlin
plugins {
    id("org.jetbrains.kotlin.test.kotlin-compiler-args-properties")
    // Other plugin declarations by the project
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.test.kotlin-compiler-args-properties")
    repositories {
        mavenLocal()
    }
}
```

This plugin should be also added similar way into any included build.

**NOTE**: Build logic (for example `buildSrc`) and projects building Gradle plugins written in Kotlin should not apply this plugin,
as Gradle Kotlin runtime most probably will not be compatible with values under-the-test (for example language version '2.0').