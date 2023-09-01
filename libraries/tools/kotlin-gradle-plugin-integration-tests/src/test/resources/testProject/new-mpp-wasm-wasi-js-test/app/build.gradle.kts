plugins {
    kotlin("multiplatform")
}

kotlin {

    wasmWasi {
        binaries.executable()
        nodejs {}
    }

    wasmJs {
        binaries.executable()
        nodejs {}
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":lib"))
            }
        }

        val wasmWasiMain by getting {
            dependencies {
                implementation(project(":lib"))
            }
        }
    }
}