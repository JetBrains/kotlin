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
    @Suppress("DEPRECATION_ERROR") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
    macosX64()
}