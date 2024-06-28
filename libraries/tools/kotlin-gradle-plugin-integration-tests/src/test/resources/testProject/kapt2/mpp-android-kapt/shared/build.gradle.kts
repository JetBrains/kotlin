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

    android {}
    
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
                implementation("androidx.hilt:hilt-navigation-compose:1.1.0-alpha01")
                implementation("com.google.dagger:hilt-android:2.4.7")
                configurations.getByName("kapt").dependencies.add(
                    org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency(
                        "com.google.dagger",
                        "hilt-compiler",
                        "2.47"
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