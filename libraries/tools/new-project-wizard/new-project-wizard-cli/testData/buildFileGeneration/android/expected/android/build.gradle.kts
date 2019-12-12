plugins {
    id("com.android.application")
    id("kotlin-android")
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
android {
    compileSdkVersion(29)
    defaultConfig {
        applicationId = "testArtifactId"
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
dependencies {
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.core:core-ktx:1.1.0")
    implementation(kotlin("stdlib-jdk7"))
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation(kotlin("stdlib-jdk8"))
}