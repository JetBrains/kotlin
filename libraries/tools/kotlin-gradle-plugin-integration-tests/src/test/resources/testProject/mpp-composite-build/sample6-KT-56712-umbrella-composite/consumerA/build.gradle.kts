plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64()
    linuxArm64()
    applyDefaultHierarchyTemplate()

    sourceSets.commonMain.get().dependencies {
        implementation("org.jetbrains.sample:producer:1.0.0-SNAPSHOT")
    }
}