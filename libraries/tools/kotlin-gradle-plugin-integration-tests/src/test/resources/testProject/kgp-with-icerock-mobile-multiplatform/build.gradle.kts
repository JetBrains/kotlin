plugins {
    id("com.android.library")
    kotlin("multiplatform")

    id("dev.icerock.mobile.multiplatform")
}

android {
    namespace = "com.example"
    compileSdk = 24
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}