plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

android {
    compileSdkVersion(26)
}

kotlin {
    android()
    jvm()
}
