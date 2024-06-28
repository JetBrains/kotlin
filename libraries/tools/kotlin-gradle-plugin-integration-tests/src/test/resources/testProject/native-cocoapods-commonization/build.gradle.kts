plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.native.cocoapods")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

kotlin {
    ios()
    cocoapods {
        homepage = "https://github.com/JetBrains/kotlin"
        summary = "CocoaPods test library"
        ios.deploymentTarget = "13.5"
        pod("AFNetworking")
    }
}
