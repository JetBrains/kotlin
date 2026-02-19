plugins {
    kotlin("android")
    id("com.android.library")
    `maven-publish`
}

android {
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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
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