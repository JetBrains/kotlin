import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.konan.target.HostManager.Companion.hostIsLinux

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
    macosX64()
    mingwX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
