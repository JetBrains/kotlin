import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.BOTH as BOTH_TYPE

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        binaries.executable()
        nodejs()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
            }
        }
    }
}