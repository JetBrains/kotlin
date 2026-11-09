/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.binaryen

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticRenderingOptions
import org.jetbrains.kotlin.gradle.plugin.diagnostics.createAnExceptionForFatalDiagnostic
import org.jetbrains.kotlin.gradle.targets.js.EnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Specification for executing Binaryen, an optimization tool for wasm files.
 */
abstract class BinaryenEnvSpec : EnvSpec<BinaryenEnv>() {
    /**
     * Specify Binaryen platform information, with name and architecture.
     * Has no value if the current platform is not supported by Binaryen.
     */
    internal abstract val platform: Property<BinaryenPlatform>

    internal abstract val diagnosticRenderingOptions: Property<ToolingDiagnosticRenderingOptions>

    final override val env: Provider<BinaryenEnv> = produceEnv()

    override val executable: Provider<String> = env.map { it.executable }

    final override fun produceEnv(): Provider<BinaryenEnv> {
        return version.map { versionValue ->
            val requiredVersionName = "binaryen-version_$versionValue"
            val targetPath = installationDirectory.getFile().resolve(requiredVersionName)
            val downloadValue = download.get()
            val platformValue: BinaryenPlatform? = platform.orNull
            if (platformValue == null && downloadValue) {
                throwUnsupportedPlatform(diagnosticRenderingOptions.get())
            }
            val isWindows = platformValue?.isWindows() ?: false

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
                // When the platform is unsupported, nothing is downloaded, so the dependency is never resolved
                ivyDependency = "com.github.webassembly:binaryen:$versionValue:${platformValue?.platform ?: "unsupported"}@tar.gz",
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

private fun throwUnsupportedPlatform(
    renderingOptions: ToolingDiagnosticRenderingOptions,
): Nothing = throw KotlinToolingDiagnostics.BinaryenUnsupportedPlatform(
    System.getProperty("os.name"),
    System.getProperty("os.arch"),
    BinaryenPlatform.SUPPORTED_PLATFORMS,
).createAnExceptionForFatalDiagnostic(renderingOptions)
