plugins {
    kotlin("multiplatform")
}

kotlin {

    wasmWasi {
        nodejs {}
    }

    wasmJs {
        nodejs {}
    }
}

the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().version.set("20.2.0")