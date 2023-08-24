import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

plugins {
    `maven-publish`
    kotlin("multiplatform")
}

description = "Kotlin Standard Library for experimental WebAssembly WASI platform"

val targetDependentSources = listOf("builtins/kotlin", "src/kotlin", "src/kotlinx").map {
    "$rootDir/libraries/stdlib/wasm/wasi/$it"
}

configureWasmStdLib(
    wasmTargetParameter = "wasm-wasi",
    wasmTargetAttribute = KotlinWasmTargetAttribute.wasi,
    targetDependentSources = targetDependentSources,
    targetDependentTestSources = listOf("$rootDir/libraries/stdlib/wasm/wasi/test/"),
    kotlinTestDependencyName = ":kotlin-test:kotlin-test-wasm-wasi"
) { extensionBody ->
    kotlin(extensionBody)
}
