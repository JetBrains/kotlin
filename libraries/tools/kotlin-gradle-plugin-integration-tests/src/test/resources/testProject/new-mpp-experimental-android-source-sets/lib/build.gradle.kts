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

    buildTypes {
        create("staging")
    }

    flavorDimensions("version")

    productFlavors {
        create("free")
        create("paid")
    }
}

kotlin {
    android()

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

        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation("com.android.support.test:runner:1.0.2")
        }

        getByName("androidLocalTest").dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}