plugins {
    kotlin("android")
    id("com.android.library")
    `maven-publish`
}

android {
    kotlinOptions {
        jvmTarget = "1.8"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    compileSdk = 31
    defaultConfig {
        minSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    namespace = "org.jetbrains.kotlin.sample"

    flavorDimensions("myFlavor")
    productFlavors {
        create("flavor1") { dimension = "myFlavor" }
    }

    publishing {
        multipleVariants {
            allVariants()
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }

    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                from(components["default"])
            }
        }
    }
}