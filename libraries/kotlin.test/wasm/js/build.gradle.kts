import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute

plugins {
    kotlin("multiplatform")
}

configureWasmKotlinTest(
    wasmTargetParameter = "wasm-js",
    wasmTargetAttribute = KotlinWasmTargetAttribute.js,
    targetSourceDir = "$rootDir/libraries/kotlin.test/wasm/js/src/main/kotlin",
    stdDependencyName = ":kotlin-stdlib"
) { extensionBody ->
    kotlin(extensionBody)
}