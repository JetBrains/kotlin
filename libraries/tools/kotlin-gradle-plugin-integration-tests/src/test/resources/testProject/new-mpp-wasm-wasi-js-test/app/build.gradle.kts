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
                implementation(project(":lib"))
            }
        }

        wasmWasiMain {
            dependencies {
                implementation(project(":lib"))
            }
        }
    }
}