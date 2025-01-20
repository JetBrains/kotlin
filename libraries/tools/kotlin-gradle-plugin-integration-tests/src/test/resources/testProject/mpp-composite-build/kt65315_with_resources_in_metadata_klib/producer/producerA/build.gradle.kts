plugins {
    kotlin("multiplatform")
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