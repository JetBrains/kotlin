@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.build.binaryen.BinaryenExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin

val binaryenRoot = BinaryenRootPlugin.apply(project.rootProject)
val binaryenEnvSpec = rootProject.the<BinaryenRootEnvSpec>()

val binaryenKotlinBuild = extensions.create<BinaryenExtension>(
    "binaryenKotlinBuild",
    binaryenRoot,
)

with(binaryenKotlinBuild) {
    binaryenEnvSpec.version.set(project.binaryenVersion)

    @Suppress("DEPRECATION")
    binaryenRoot.version = project.binaryenVersion
}