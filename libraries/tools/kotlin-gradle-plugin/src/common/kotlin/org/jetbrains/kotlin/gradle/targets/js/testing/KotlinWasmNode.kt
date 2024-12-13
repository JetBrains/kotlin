/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.writeWasmUnitTestRunner

internal class KotlinWasmNode(kotlinJsTest: KotlinJsTest) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasmNode"

    private val testPath = kotlinJsTest.path

    @Transient
    private val nodeJs = kotlinJsTest.project.kotlinNodeJsEnvSpec

    @Transient
    override val compilation: KotlinJsIrCompilation = kotlinJsTest.compilation

    private val projectLayout = kotlinJsTest.project.layout

    override val workingDir: Provider<Directory> =
        if (compilation.target.wasmTargetType != KotlinWasmTargetType.WASI) {
            compilation.npmProject.dir
        } else {
            projectLayout.dir(kotlinJsTest.inputFileProperty.asFile.map { it.parentFile })
        }


    override val executable: Provider<String> = nodeJs.executable

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean,
    ): TCServiceMessagesTestExecutionSpec {
        val testRunnerFile = writeWasmUnitTestRunner(workingDir.get().asFile, task.inputFileProperty.get().asFile)

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
            addAll(nodeJsArgs)
            add(testRunnerFile.absolutePath)
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