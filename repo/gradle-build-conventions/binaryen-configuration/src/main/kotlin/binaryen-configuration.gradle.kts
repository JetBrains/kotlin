@file:OptIn(ExperimentalWasmDsl::class)
// because imports are deprecated
@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.build.binaryen.BinaryenExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin

project.rootProject.plugins.apply(BinaryenRootPlugin::class.java)
val binaryenEnvSpec = project.rootProject.the<BinaryenRootEnvSpec>()

val binaryenKotlinBuild = extensions.create<BinaryenExtension>(
    "binaryenKotlinBuild",
    binaryenEnvSpec,
)

with(binaryenKotlinBuild) {
    binaryenEnvSpec.version.set(project.binaryenVersion)
}