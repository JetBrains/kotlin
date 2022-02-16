plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
    `maven-publish`
}

group = "kt50925"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    linuxX64()
    linuxArm64()
}

publishing {
    repositories {
        maven("$rootDir/../repo")
    }
}
