@file:OptIn(ComposeKotlinGradlePluginApi::class)

import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import java.io.File

group = "test"
version = "1.0"

plugins {
    kotlin("multiplatform")
    id("com.android.application")
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    jvm()
    linuxX64()
    wasmJs()
    wasmWasi()
    js()
    iosArm64()
    iosSimulatorArm64()

    sourceSets.commonMain {
        dependencies {
            implementation("test:publication:+")
            implementation(project(":project"))
        }
    }
}

android {
    namespace = "test.dependency"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}