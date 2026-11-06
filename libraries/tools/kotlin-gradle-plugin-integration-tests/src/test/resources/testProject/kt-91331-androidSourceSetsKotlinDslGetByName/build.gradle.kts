plugins {
    id("com.android.library")
    kotlin("android")
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
}

android {
    namespace = "com.example"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}

@Suppress("DEPRECATION_ERROR")
kotlin {
    sourceSets {
        getByName("main")
    }
}
