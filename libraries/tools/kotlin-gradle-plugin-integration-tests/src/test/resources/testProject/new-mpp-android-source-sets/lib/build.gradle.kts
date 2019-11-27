plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

android {
    compileSdkVersion(28)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
}

kotlin {
    android()
    macosX64("macos")

    sourceSets {
        getByName("commonMain").dependencies {
            implementation(kotlin("stdlib-common"))
        }

        getByName("commonMain").dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-annotations-common"))
        }

        getByName("androidMain").dependencies {
            implementation(kotlin("stdlib-jdk8"))
        }

        getByName("androidAndroidTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation("com.android.support.test:runner:1.0.2")
        }
    }
}