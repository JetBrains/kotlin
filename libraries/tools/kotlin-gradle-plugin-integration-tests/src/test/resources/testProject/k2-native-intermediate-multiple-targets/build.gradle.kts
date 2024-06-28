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
    macosX64()
    macosArm64()
    mingwX64()
    androidNativeX64()
}
