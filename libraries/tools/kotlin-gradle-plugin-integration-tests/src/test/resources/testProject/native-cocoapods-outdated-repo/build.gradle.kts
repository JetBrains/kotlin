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
    sourceSets["commonMain"].dependencies {
        implementation(kotlin("stdlib-common"))
    }
    iosX64("iOS")
    cocoapods {
        homepage = "https://github.com/JetBrains/kotlin"
        summary = "CocoaPods test library"
        ios.deploymentTarget = "13.5"
    }
}
