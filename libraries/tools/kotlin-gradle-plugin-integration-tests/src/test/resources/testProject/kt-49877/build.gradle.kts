plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

android {
    compileSdk = 30
}

kotlin {
    android()
    linuxX64()

    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation("com.squareup.okio:okio:3.2.0")
            }
        }
    }
}
