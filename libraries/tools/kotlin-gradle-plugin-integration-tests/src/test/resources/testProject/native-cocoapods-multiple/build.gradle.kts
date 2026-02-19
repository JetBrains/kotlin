plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.native.cocoapods")
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

kotlin {
    iosArm64()
    iosSimulatorArm64()
}
