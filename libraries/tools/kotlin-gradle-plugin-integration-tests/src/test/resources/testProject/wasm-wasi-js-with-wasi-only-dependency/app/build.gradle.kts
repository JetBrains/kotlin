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
        wasmJsMain {
            dependencies {
            }
        }

        wasmWasiMain {
            dependencies {
                implementation(project(":lib"))
            }
        }
    }
}