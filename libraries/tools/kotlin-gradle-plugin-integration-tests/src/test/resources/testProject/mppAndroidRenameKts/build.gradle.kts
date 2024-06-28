plugins {
    id("com.android.library")
    kotlin("multiplatform")
}


android {
    compileSdk = 31
    namespace = "org.jetbrains.kotlin.sample"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

kotlin {
    android()
}
