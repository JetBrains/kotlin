/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {

    namespace = "org.jetbrains.kotlinx.android.kotlin.library"

    compileSdk = 32

    defaultConfig {
        minSdk = 31
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }

}

dependencies {
    // no dependencies required
}
