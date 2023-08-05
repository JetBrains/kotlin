import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

plugins {
    `maven-publish`
    kotlin("multiplatform")
}

description = "Kotlin Standard Library for experimental WebAssembly JS platform"

D8RootPlugin.apply(rootProject).version = v8Version

val targetDependentSources = listOf("builtins/kotlin", "internal", "src/kotlin", "src/kotlinx", "src/org.w3c").map {
    "$rootDir/libraries/stdlib/wasm/js/$it"
}

configureWasmStdLib(
    wasmTargetParameter = "wasm-js",
    wasmTargetAttribute = KotlinWasmTargetAttribute.js,
    targetDependentSources = targetDependentSources,
    targetDependentTestSources = listOf("$rootDir/libraries/stdlib/wasm/js/test/"),
    kotlinTestDependencyName = ":kotlin-test:kotlin-test-wasm-js"
) { extensionBody ->
    kotlin(extensionBody)
}
