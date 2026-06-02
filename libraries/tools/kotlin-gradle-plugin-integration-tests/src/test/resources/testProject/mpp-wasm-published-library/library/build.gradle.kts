plugins {
    kotlin("multiplatform")
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example.mpp-wasm-published-library"
version = "0.0.1"

kotlin {
    wasmJs {
        nodejs {
        }
    }

    sourceSets {
        val wasmJsMain = getByName("wasmJsMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
    }
}

publishing {
    repositories {
        maven("repo")
    }
}