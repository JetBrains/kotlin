@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.build.binaryen.BinaryenExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin

project.plugins.apply(BinaryenPlugin::class.java)
val binaryenEnvSpec = project.the<BinaryenEnvSpec>()

val binaryenKotlinBuild = extensions.create<BinaryenExtension>(
    "binaryenKotlinBuild",
    binaryenEnvSpec,
)

with(binaryenKotlinBuild) {
    binaryenEnvSpec.version.set(binaryenVersion)
}