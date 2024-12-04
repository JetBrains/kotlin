// ISSUE: KT-61778

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
    macosArm64()
    macosX64()
}