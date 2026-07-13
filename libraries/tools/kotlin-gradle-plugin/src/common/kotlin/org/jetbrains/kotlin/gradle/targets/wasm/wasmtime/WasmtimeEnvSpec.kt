/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtime

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Spec for Wasmtime - this target is available only for Wasm
 */
@ExperimentalWasmDsl
abstract class WasmtimeEnvSpec internal constructor() : EnvSpec<WasmtimeEnv>() {

    final override val env: Provider<WasmtimeEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    final override fun produceEnv(): Provider<WasmtimeEnv> {
        // probably better to reorganize with multiple zip
        // KT-87230
        return version.map { versionValue ->
            val classifier = WasmtimePlatform.platform
            val archiveExtension = WasmtimePlatform.archiveExtension
            val requiredVersionName = "wasmtime-v$versionValue-$classifier"
            val targetPath = installationDirectory.getFile().resolve(requiredVersionName)
            val isWindows = WasmtimePlatform.name == WasmtimePlatform.WINDOWS

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

            WasmtimeEnv(
                download = downloadValue,
                downloadBaseUrl = downloadBaseUrl.orNull,
                allowInsecureProtocol = allowInsecureProtocol.get(),
                ivyDependency = "bytecodealliance.wasmtime:wasmtime:$versionValue:$classifier@$archiveExtension",
                executable = getExecutable("wasmtime", command.get(), "exe"),
                dir = targetPath,
                isWindows = isWindows,
            )
        }
    }

    val Project.wasmtimeSetupTaskProvider: TaskProvider<out WasmtimeSetupTask>
        get() = project.tasks.withType(WasmtimeSetupTask::class.java)
            .named(
                WasmPlatformDisambiguator.extensionName(
                    WasmtimeSetupTask.BASE_NAME,
                )
            )

    companion object : HasPlatformDisambiguator by WasmPlatformDisambiguator {
        val EXTENSION_NAME: String
            get() = extensionName(
                "WasmtimeSpec"
            )
    }
}
