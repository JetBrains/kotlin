plugins {
    id("com.android.application")
    kotlin("android") version "1.3.70"
    id("kotlin-android-extensions")
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    google()
    jcenter()
    mavenCentral()
}
dependencies {
    implementation("androidx.core:core-ktx:1.1.0")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation(kotlin("stdlib-jdk7"))
}
android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "testGroupId.android"
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