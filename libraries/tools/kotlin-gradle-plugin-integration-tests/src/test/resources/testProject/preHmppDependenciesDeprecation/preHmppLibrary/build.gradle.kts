plugins {
    kotlin("multiplatform") version "2.0.0-RC2"
    id("maven-publish")
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "org.jetbrains.kotlin.tests"
version = "0.1"

kotlin {
    jvm()
    js()
    linuxX64()
}
