@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import wasmtime.wasmtime

plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmWasi {
        binaries.executable()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}
