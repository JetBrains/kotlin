plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(projectDir.resolve("../library/repo").toURI())
}

kotlin {
    wasmJs {
        nodejs {
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation("com.example.mpp-wasm-published-library:library:0.0.1")
            }
        }
    }
}