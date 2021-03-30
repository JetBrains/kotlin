plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("org.jetbrains.kotlin.native.cocoapods").version("<pluginMarkerVersion>")
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
}
