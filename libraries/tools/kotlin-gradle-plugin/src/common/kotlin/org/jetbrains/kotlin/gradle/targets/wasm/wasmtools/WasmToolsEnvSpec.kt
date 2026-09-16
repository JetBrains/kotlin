/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Spec for `wasm-tools` - this tool is available only for Wasm
 */
@ExperimentalWasmDsl
abstract class WasmToolsEnvSpec internal constructor() : EnvSpec<WasmToolsEnv>() {

    final override val env: Provider<WasmToolsEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    final override fun produceEnv(): Provider<WasmToolsEnv> {
        return version.map { versionValue ->
            val classifier = WasmToolsPlatform.platform
            val archiveExtension = WasmToolsPlatform.archiveExtension
            val requiredVersionName = "wasm-tools-$versionValue-$classifier"
            val targetPath = installationDirectory.getFile().resolve(requiredVersionName)
            val isWindows = WasmToolsPlatform.name == WasmToolsPlatform.WINDOWS

            val downloadValue = download.get()
            fun getExecutable(command: String, customCommand: String, windowsExtension: String): String {
                val finalCommand =
                    if (isWindows && customCommand == command) "$command.$windowsExtension" else customCommand
                return if (downloadValue)
                    targetPath
                        .resolve(finalCommand)
                        .absolutePath
                else
                    finalCommand
            }

            WasmToolsEnv(
                download = downloadValue,
                downloadBaseUrl = downloadBaseUrl.orNull,
                allowInsecureProtocol = allowInsecureProtocol.get(),
                ivyDependency = "bytecodealliance.wasm-tools:wasm-tools:$versionValue:$classifier@$archiveExtension",
                executable = getExecutable("wasm-tools", command.get(), "exe"),
                dir = targetPath,
                isWindows = isWindows,
            )
        }
    }

    val Project.wasmToolsSetupTaskProvider: TaskProvider<out WasmToolsSetupTask>
        get() = project.tasks.withType(WasmToolsSetupTask::class.java)
            .named(
                WasmPlatformDisambiguator.extensionName(
                    WasmToolsSetupTask.BASE_NAME,
                )
            )

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName(
                "WasmToolsSpec"
            )
    }
}
