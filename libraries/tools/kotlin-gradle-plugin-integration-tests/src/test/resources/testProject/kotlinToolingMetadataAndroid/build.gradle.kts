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
    namespace = "kotlin.multiplatform.projects"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    android()
}
