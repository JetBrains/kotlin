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
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    macosX64()
    mingwX64()
    tvosArm64()
    tvosSimulatorArm64()
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    tvosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    watchosX64()
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
