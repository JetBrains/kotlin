plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    android()

    ios() {
        binaries {
            framework {
                baseName = "shared"
            }
        }
    }
}

android {
    compileSdkVersion(30)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}
