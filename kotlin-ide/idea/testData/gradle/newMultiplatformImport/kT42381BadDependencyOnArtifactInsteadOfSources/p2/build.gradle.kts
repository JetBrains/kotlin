plugins {
    kotlin("multiplatform")
    id("com.android.library")
}
android {
    compileSdkVersion(26)
}
kotlin {
    js() // arbitrary
    jvm()
    android()
}
