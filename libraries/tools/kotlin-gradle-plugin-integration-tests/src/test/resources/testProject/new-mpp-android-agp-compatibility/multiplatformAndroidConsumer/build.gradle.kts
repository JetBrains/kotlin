plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

repositories {
    maven(rootProject.buildDir.resolve("repo"))
    mavenCentral()
}

android {
    compileSdkVersion(30)
}

kotlin {
    android()

    sourceSets.commonMain.get().dependencies {
        implementation("com.example:producer:1.0.0-SNAPSHOT")
    }
}
