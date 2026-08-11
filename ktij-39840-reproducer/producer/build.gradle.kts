plugins {
    kotlin("multiplatform") version "2.3.21"
    `maven-publish`
}

group = "ru.quickresto"
version = "3.0.0"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    // Default hierarchy supplies iosMain shared by iosArm64 + iosSimulatorArm64.
    sourceSets {
        commonMain.dependencies {
            // no external deps — tiny public API only
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
