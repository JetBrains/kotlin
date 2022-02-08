plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("org.jetbrains.kotlin.native.cocoapods").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

kotlin {
    iosX64()
    cocoapods {
        homepage = "https://github.com/JetBrains/kotlin"
        summary = "CocoaPods test library"
        ios.deploymentTarget = "13.5"
        pod("AFNetworking")
    }
}
