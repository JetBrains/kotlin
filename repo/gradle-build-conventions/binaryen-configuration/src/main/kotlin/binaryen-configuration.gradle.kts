@file:OptIn(ExperimentalWasmDsl::class)
@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.build.binaryen.BinaryenExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootExtension
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin

project.rootProject.plugins.apply(BinaryenRootPlugin::class.java)
val binaryenRoot = rootProject.the<BinaryenRootEnvSpec>()
val binaryenSetupTask = rootProject.the<BinaryenRootExtension>().setupTaskProvider

extensions.create<BinaryenExtension>(
    "binaryenKotlinBuild",
    binaryenRoot,
    binaryenSetupTask
)