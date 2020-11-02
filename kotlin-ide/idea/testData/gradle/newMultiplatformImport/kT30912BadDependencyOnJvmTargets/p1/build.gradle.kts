plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(26)
}

dependencies {
    implementation(project(":p2"))
}
