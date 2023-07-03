
plugins {
    id("org.jetbrains.kotlin.test.fixes.android")

    kotlin("multiplatform").apply(false)
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}