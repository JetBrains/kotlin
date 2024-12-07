/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.writeWasmUnitTestRunner

@ExperimentalWasmDsl
internal class KotlinWasmD8(kotlinJsTest: KotlinJsTest) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasmD8"

    private val testPath = kotlinJsTest.path

    @Transient
    override val compilation: KotlinJsIrCompilation = kotlinJsTest.compilation

    private val projectLayout = kotlinJsTest.project.layout
    private val d8 = D8Plugin.applyWithEnvSpec(kotlinJsTest.project)

    override val workingDir: Provider<Directory> = projectLayout.dir(kotlinJsTest.inputFileProperty.asFile.map { it.parentFile })

    override val executable: Provider<String> = d8.executable

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean
    ): TCServiceMessagesTestExecutionSpec {
        val compiledFile = task.inputFileProperty.get().asFile
        val testRunnerFile = writeWasmUnitTestRunner(workingDir.get().asFile, compiledFile)

        forkOptions.workingDir = compiledFile.parentFile

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

        val args = mutableListOf<String>()
        with(args) {
            add(testRunnerFile.absolutePath)
            add("--")
            addAll(cliArgs.toList())
        }

        return TCServiceMessagesTestExecutionSpec(
            forkOptions = forkOptions,
            args = args,
            checkExitCode = false,
            clientSettings = clientSettings,
            dryRunArgs = args + "--dryRun"
        )
    }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = emptySet()

    override fun getPath(): String = "$testPath:kotlinTestFrameworkStub"
}