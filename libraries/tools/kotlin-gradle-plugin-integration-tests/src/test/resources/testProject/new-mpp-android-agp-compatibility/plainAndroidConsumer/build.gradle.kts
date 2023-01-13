plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(30)
}

repositories {
    maven("<localRepo>")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk7"))
    implementation("com.example:producer:1.0.0-SNAPSHOT")
}
