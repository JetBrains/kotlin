import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

plugins {
    kotlin("multiplatform")
}

configureWasmKotlinTest(
    wasmTargetParameter = "wasm-wasi",
    wasmTargetAttribute = KotlinWasmTargetAttribute.wasi,
    targetSourceDir = "$rootDir/libraries/kotlin.test/wasm/wasi/src/main/kotlin",
    stdDependencyName = ":kotlin-stdlib-wasm-wasi"
) { extensionBody ->
    kotlin(extensionBody)
}