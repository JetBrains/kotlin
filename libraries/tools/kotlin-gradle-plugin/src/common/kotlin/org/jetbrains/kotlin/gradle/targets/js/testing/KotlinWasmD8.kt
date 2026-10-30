/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.writeWasmUnitTestRunner
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin
import org.jetbrains.kotlin.gradle.toolchains.d8Toolchain
import org.jetbrains.kotlin.gradle.utils.processes.ProcessLaunchOptions

@OptIn(ExperimentalKotlinToolchainDsl::class)
@ExperimentalWasmDsl
internal class KotlinWasmD8(
    kotlinJsTest: KotlinJsTest,
) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasmD8"

    private val testPath = kotlinJsTest.path

    @Transient
    override val compilation: KotlinJsIrCompilation = kotlinJsTest.compilation

    private val projectLayout = kotlinJsTest.project.layout
    private val d8 = D8Plugin.applyWithEnvSpec(kotlinJsTest.project)
    private val d8ToolchainService = d8Toolchain.registerIfAbsent(kotlinJsTest.project)
    private val d8Request = d8Toolchain.defaultRequest(kotlinJsTest.project)

    override val workingDir: Provider<Directory> = projectLayout.dir(kotlinJsTest.inputFileProperty.asFile.map { it.parentFile })

    // Resolved lazily, because the D8 toolchain provisions D8 only when the executable is actually needed.
    override val executable: Provider<String> = d8ToolchainService.map {
        d8Toolchain.resolveExecutable(it, d8.executable, d8Request)
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        launchOpts: ProcessLaunchOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec {
        val compiledFile = task.inputFileProperty.get().asFile
        val testRunnerFile = writeWasmUnitTestRunner(launchOpts.workingDir.get().asFile, compiledFile)

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
        )

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        val args = mutableListOf<String>().apply {
            add(testRunnerFile.absolutePath)
            add("--")
            addAll(cliArgs.toList())
        }

        launchOpts.workingDir.set(compiledFile.parentFile)

        return TCServiceMessagesTestExecutionSpec(
            processLaunchOptions = launchOpts,
            processArgs = args,
            checkExitCode = false,
            clientSettings = clientSettings,
            dryRunArgs = args + "--dryRun"
        )
    }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = emptySet()

    @Deprecated("No longer used. Scheduled for removal in Kotlin 2.7.")
    override fun getPath(): String = "$testPath:kotlinTestFrameworkStub"
}
