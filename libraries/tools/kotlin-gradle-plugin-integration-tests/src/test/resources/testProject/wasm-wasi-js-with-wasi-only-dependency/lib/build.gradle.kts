plugins {
    kotlin("multiplatform")
}

kotlin {

    wasmWasi {
        nodejs {}
    }
}

the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().version.set("20.2.0")