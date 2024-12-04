@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

android {
    compileSdk = 34
    defaultConfig {
        minSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    namespace = "org.jetbrains.kotlin.sample"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    android.flavorDimensions.add("market")
    android.flavorDimensions.add("price")
    android.productFlavors.create("german").dimension = "market"
    android.productFlavors.create("usa").dimension = "market"
    android.productFlavors.create("paid").dimension = "price"
    android.productFlavors.create("free").dimension = "price"
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(17)
    androidTarget {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    }

    val commonMain by sourceSets.getting
    val commonTest by sourceSets.getting
    val androidMain by sourceSets.getting

    val androidUnitTest by sourceSets.getting
    val androidInstrumentedTest by sourceSets.getting

    commonTest.dependencies {
        implementation(kotlin("test-junit"))
    }

    androidUnitTest.dependencies {
        implementation("org.robolectric:robolectric:4.8")
        implementation("androidx.test:core:1.4.0")
        implementation("androidx.test:core-ktx:1.4.0")
    }

    androidInstrumentedTest.dependencies {
        implementation("androidx.test:runner:1.4.0")
        implementation("androidx.test:rules:1.4.0")
    }

    sourceSets.invokeWhenCreated("androidUnitTestGermanFreeDebug") {
        dependencies {
            implementation("com.squareup.okio:okio:3.2.0")
        }
    }
}
