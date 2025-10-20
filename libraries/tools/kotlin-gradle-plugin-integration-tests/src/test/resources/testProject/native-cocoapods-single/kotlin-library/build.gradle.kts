plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.native.cocoapods")
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

kotlin {
    iosArm64()
    iosSimulatorArm64()
    cocoapods {
        summary = "CocoaPods test library"
        homepage = "https://github.com/JetBrains/kotlin"
        pod("pod_dependency", "1.0", project.file("../pod_dependency"))
        pod("subspec_dependency/Core", "1.0", project.file("../subspec_dependency"))
        podfile = project.file("../ios-app/Podfile")

        ios.deploymentTarget = "15.0"
    }
}
