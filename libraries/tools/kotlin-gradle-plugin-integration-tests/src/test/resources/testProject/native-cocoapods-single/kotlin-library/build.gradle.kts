plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.native.cocoapods")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

kotlin {
    iosX64("iOS")

    cocoapods {
        summary = "CocoaPods test library"
        homepage = "https://github.com/JetBrains/kotlin"
        // test warnIfDeprecatedPodspecPathIsUsed depends on deprecated api usage
        pod("pod_dependency", "1.0", project.file("../pod_dependency/pod_dependency.podspec"))
        pod("subspec_dependency/Core", "1.0", project.file("../subspec_dependency"))
        podfile = project.file("../ios-app/Podfile")

        ios.deploymentTarget = "11.0"
    }
}
