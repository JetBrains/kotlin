/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.binaryen

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnv
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Specification for executing Binaryen, an optimization tool for wasm files.
 */
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
            val cleanableStore = CleanableStore[installationDirectory.getFile().absolutePath]
            val targetPath = cleanableStore[requiredVersionName].use()
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
                cleanableStore = cleanableStore,
                isWindows = isWindows,
            )
        }
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinBinaryenSpec"
    }
}

@OptIn(ExperimentalWasmDsl::class)
typealias BinaryenRootEnvSpec = BinaryenEnvSpec
