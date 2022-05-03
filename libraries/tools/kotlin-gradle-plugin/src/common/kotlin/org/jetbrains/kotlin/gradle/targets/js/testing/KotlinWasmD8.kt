/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.isTeamCity
import org.jetbrains.kotlin.gradle.utils.getValue

internal class KotlinWasmD8(private val kotlinJsTest: KotlinJsTest) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasmD8"
    override val compilation: KotlinJsCompilation = kotlinJsTest.compilation
    private val project: Project = compilation.target.project
    private val d8 = D8RootPlugin.apply(project.rootProject)
    private val d8Executable by project.provider { d8.requireConfigured().executablePath }

    init {
        kotlinJsTest.outputs.upToDateWhen { false }
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean
    ): TCServiceMessagesTestExecutionSpec {
        val compiledFile = task.inputFileProperty.get().asFile

        forkOptions.executable = d8Executable.absolutePath
        forkOptions.workingDir = compiledFile.parentFile

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
            escapeTCMessagesInLog = project.isTeamCity
        )

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        val args = mutableListOf(
            "--module",
            compiledFile.absolutePath,
            "--experimental-wasm-typed-funcref",
            "--experimental-wasm-gc",
            "--experimental-wasm-eh",
        )

        args.add("--")
        args.addAll(cliArgs.toList())

        val dryRunArgs = mutableListOf<String>()
        dryRunArgs.addAll(args)
        dryRunArgs.add("--dryRun")

        return TCServiceMessagesTestExecutionSpec(
            forkOptions = forkOptions,
            args = args,
            checkExitCode = false,
            clientSettings = clientSettings,
            dryRunArgs = dryRunArgs
        )
    }

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency> = emptySet()

    override fun getPath(): String = "${kotlinJsTest.path}:kotlinTestFrameworkStub"
}