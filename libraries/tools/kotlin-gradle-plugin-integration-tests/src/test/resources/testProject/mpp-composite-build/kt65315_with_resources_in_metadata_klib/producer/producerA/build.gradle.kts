plugins {
    kotlin("multiplatform") version "1.9.23" // In this version resources were published inside metadata klibs
    `maven-publish`
}


group = "org.jetbrains.sample"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvm()
    iosX64()
    iosArm64()
    linuxX64()
}