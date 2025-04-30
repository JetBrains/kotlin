/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnv
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlatform
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenSetupTask
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Specification for executing Binaryen, an optimization tool for wasm files.
 */
@Deprecated(
    "Use 'org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec' instead",
    ReplaceWith(
        "BinaryenEnvSpec",
        "org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec"
    )
)
@ExperimentalWasmDsl
abstract class BinaryenEnvSpec : EnvSpec<BinaryenEnv>() {

    /**
     * Specify Binaryen platform information, with name and architecture.
     */
    internal abstract val platform: Property<BinaryenPlatform>

    final override val env: Provider<BinaryenEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    final override fun produceEnv(): Provider<BinaryenEnv> {
        return version.map { versionValue ->
            val requiredVersionName = "binaryen-version_$versionValue"
            val targetPath = installationDirectory.getFile().resolve(requiredVersionName)
            val platformValue = platform.get()
            val isWindows = platformValue.isWindows()

            val downloadValue = download.get()
            fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
                val finalCommand =
                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                return if (downloadValue)
                    targetPath
                        .resolve("bin")
                        .resolve(finalCommand)
                        .absolutePath
                else
                    finalCommand
            }

            BinaryenEnv(
                download = downloadValue,
                downloadBaseUrl = downloadBaseUrl.orNull,
                allowInsecureProtocol = allowInsecureProtocol.get(),
                ivyDependency = "com.github.webassembly:binaryen:$versionValue:${platformValue.platform}@tar.gz",
                executable = getExecutable("wasm-opt", command.get(), "exe"),
                dir = targetPath,
                isWindows = isWindows,
            )
        }
    }

    val Project.binaryenSetupTaskProvider: TaskProvider<out BinaryenSetupTask>
        get() = project.tasks.withType(BinaryenSetupTask::class.java)
            .named(
                WasmPlatformDisambiguator.extensionName(
                    BinaryenSetupTask.BASE_NAME,
                )
            )

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName("binaryenSpec")
    }
}

@OptIn(ExperimentalWasmDsl::class)
typealias BinaryenRootEnvSpec = BinaryenEnvSpec
