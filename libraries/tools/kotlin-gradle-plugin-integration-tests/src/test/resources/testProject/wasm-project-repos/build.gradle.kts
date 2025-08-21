plugins {
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        binaries.executable()
        nodejs {
        }
        d8()
    }
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.d8.D8EnvSpec>().downloadBaseUrl.set(null as String?)
}