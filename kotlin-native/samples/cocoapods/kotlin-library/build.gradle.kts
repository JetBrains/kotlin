plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.native.cocoapods")
}

repositories {
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

kotlin {
    // Add a platform switching to have an IDE support.
    val buildForDevice = project.findProperty("kotlin.native.cocoapods.target") == "ios_arm"
    if (buildForDevice) {
        iosArm64("iOS64")
        iosArm32("iOS32")

        val iOSMain by sourceSets.creating
        sourceSets["iOS64Main"].dependsOn(iOSMain)
        sourceSets["iOS32Main"].dependsOn(iOSMain)
    } else {
        iosX64("iOS")
    }

    cocoapods {
        // Configure fields required by CocoaPods.
        summary = "Working with AFNetworking from Kotlin/Native using CocoaPods"
        homepage = "https://github.com/JetBrains/kotlin-native"
        
        // Configure a dependency on AFNetworking. It will be added in all macOS and iOS targets.
        pod("AFNetworking", "~> 3.2.0")
    }
}
