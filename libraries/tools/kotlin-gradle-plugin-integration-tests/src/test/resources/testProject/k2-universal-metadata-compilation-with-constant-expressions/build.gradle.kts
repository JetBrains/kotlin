// ISSUE: KT-63835

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()

    js()

    linuxX64()

    macosArm64()
    macosX64()

    iosArm64()
    iosSimulatorArm64()
}