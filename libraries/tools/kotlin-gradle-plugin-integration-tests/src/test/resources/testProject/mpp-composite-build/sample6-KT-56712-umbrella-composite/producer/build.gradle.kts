plugins {
    kotlin("multiplatform")

    /* Loading a second plugin is necessary to ensure ClassLoader isolation */
    id("com.android.library")

}

repositories {
    mavenLocal()
    mavenCentral()
}

android {
    compileSdk = 33
}

kotlin {
    android()
    jvm()
    linuxX64()
    linuxArm64()
    targetHierarchy.default()
}

group = "org.jetbrains.sample"
version = "1.0.0-SNAPSHOT"
