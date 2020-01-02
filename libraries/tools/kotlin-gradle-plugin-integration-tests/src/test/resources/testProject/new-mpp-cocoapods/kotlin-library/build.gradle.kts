plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("org.jetbrains.kotlin.native.cocoapods").version("<pluginMarkerVersion>")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx.html/") }
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

kotlin {
    iosX64("iOS")

    cocoapods {
        summary = "CocoaPods test library"
        homepage = "https://github.com/JetBrains/kotlin"
        pod("pod_dependency", "1.0")
        pod("subspec_dependency/Core", "1.0")
    }
}
