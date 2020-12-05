plugins {
    id("org.jetbrains.compose") version "0.1.0-dev106"
    id("com.android.application")
    kotlin("android")
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    google()
}

dependencies {
    implementation(project(":common"))
}

android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "me.user.android"
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}