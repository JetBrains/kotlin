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

the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().version.set("20.2.0")