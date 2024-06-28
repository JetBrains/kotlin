plugins {
    id("com.android.library")
    kotlin("multiplatform")

    id("dev.icerock.mobile.multiplatform")
}

android {
    namespace = "com.example"
    compileSdk = 24
    kotlin {
        jvmToolchain(8)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}