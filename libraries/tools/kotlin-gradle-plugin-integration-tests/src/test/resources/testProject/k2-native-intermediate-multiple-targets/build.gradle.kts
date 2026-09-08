group = "test"
version = "1.0"

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    applyDefaultHierarchyTemplate()
    linuxX64()
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    macosX64()
    macosArm64()
    mingwX64()
    androidNativeX64()
}
