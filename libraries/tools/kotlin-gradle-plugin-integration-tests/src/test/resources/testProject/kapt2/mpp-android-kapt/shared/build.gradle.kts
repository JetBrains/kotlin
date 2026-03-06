plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("kapt")
}

repositories {
    mavenLocal()
    google()
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    androidTarget()
    
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
                implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
                implementation("com.google.dagger:hilt-android:2.51.1")
                configurations.getByName("kapt").dependencies.add(
                    org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency(
                        "com.google.dagger",
                        "hilt-compiler",
                        "2.52"
                    )
                )
            }
        }
    }
}

android {
    namespace = "hilt.error.sampleapp"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}