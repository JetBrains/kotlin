plugins {
    kotlin("multiplatform")
}

kotlin {
    androidNativeArm64()
    androidNativeX64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    linuxArm64()
    linuxX64()
    macosArm64()
    mingwX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
