plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

group = "com.jetbrains.library"
version = "1.0"

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

publishing {
    repositories {
        maven {
            url = layout.buildDirectory.dir("repo").get().asFile.toURI()
        }
    }
}

repositories {
    mavenCentral()
}