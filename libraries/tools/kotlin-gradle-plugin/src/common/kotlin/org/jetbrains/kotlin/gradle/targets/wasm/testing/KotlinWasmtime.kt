/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.testing

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtime.WasmtimePlugin
import org.jetbrains.kotlin.gradle.targets.wasm.wasmtimeInvokeArgs
import org.jetbrains.kotlin.gradle.toolchains.wasmtimeToolchain
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions

@OptIn(ExperimentalKotlinToolchainDsl::class)
@ExperimentalWasmDsl
internal class KotlinWasmtime(
    kotlinJsTest: KotlinJsTest,
) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasmtime"

    private val testPath = kotlinJsTest.path

    @Transient
    override val compilation: KotlinJsIrCompilation = kotlinJsTest.compilation

    private val projectLayout = kotlinJsTest.project.layout
    private val wasmtime = WasmtimePlugin.applyWithEnvSpec(kotlinJsTest.project)
    private val wasmtimeToolchainService = wasmtimeToolchain.registerIfAbsent(kotlinJsTest.project)
    private val wasmtimeRequest = wasmtimeToolchain.defaultRequest(kotlinJsTest.project)

    override val workingDir: Provider<Directory> =
        projectLayout.dir(kotlinJsTest.inputFileProperty.asFile.map { it.parentFile })

    // Resolved lazily, because the Wasmtime toolchain provisions Wasmtime only when the executable is needed.
    override val executable: Provider<String> = wasmtimeToolchainService.map {
        wasmtimeToolchain.resolveExecutable(it, wasmtime.executable, wasmtimeRequest)
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec {
        val compiledFile = task.inputFileProperty.getFile()

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
        )

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns,
        )

        val args = mutableListOf<String>().apply {
            addAll(wasmtimeInvokeArgs("startUnitTests"))
            add(compiledFile.absolutePath)
            addAll(cliArgs.toList())
        }

        launchOpts.workingDir.set(compiledFile.parentFile)

        return TCServiceMessagesTestExecutionSpec(
            processLaunchOptions = launchOpts,
            processArgs = args,
            checkExitCode = false,
            clientSettings = clientSettings,
            dryRunArgs = args + "--dryRun",
        )
    }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = emptySet()

    @Deprecated("No longer used. Scheduled for removal in Kotlin 2.7.")
    override fun getPath(): String = "$testPath:kotlinTestFrameworkStub"
}
