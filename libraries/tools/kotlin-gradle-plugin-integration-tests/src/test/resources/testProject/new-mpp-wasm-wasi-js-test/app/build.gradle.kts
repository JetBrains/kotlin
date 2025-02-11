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

the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version.set("20.2.0")