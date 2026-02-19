plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "org.bug.library"
        compileSdk = 36
        minSdk = 28
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
}