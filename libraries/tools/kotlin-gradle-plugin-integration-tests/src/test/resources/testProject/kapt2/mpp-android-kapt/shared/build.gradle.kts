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
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
                implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
                implementation("com.google.dagger:hilt-android:2.59.1")
                configurations.getByName("kapt").dependencies.add(
                    project.dependencies.create(
                        "com.google.dagger:hilt-compiler:2.59.1"
                    )
                )
                val kotlin_version: String by project.extra
                configurations.getByName("kapt").dependencies.add(
                    project.dependencies.create(
                        "org.jetbrains.kotlin:kotlin-metadata-jvm:$kotlin_version"
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