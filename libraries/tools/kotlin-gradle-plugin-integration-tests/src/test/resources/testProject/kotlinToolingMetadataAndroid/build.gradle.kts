plugins {
    id("com.android.application")
    kotlin("multiplatform")
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
}

android {
    compileSdkVersion(23)
    buildToolsVersion("25.0.2")
}

kotlin {
    android()
}
