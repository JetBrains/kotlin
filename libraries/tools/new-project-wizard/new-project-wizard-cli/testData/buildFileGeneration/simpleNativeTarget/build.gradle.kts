plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}

group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("KOTLIN_REPO")
}

kotlin {
    linuxX64()
    sourceSets {
        val linuxX64Main by getting
        val linuxX64Test by getting
    }
}