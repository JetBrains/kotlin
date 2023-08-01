/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.addWasmExperimentalArguments
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.writeWasmUnitTestRunner
import org.jetbrains.kotlin.gradle.utils.doNotTrackStateCompat
import org.jetbrains.kotlin.gradle.utils.getValue
import java.nio.file.Path

internal class KotlinWasmD8(private val kotlinJsTest: KotlinJsTest) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasmD8"
    @Transient
    override val compilation: KotlinJsCompilation = kotlinJsTest.compilation
    @Transient
    private val project: Project = compilation.target.project

    private val d8 = D8RootPlugin.apply(project.rootProject)
    private val d8Executable by project.provider { d8.requireConfigured().executablePath }
    private val isTeamCity = project.providers.gradleProperty(TCServiceMessagesTestExecutor.TC_PROJECT_PROPERTY)
    private val npmProjectDir by project.provider { compilation.npmProject.dir }

    override val workingDir: Path
        get() = npmProjectDir.toPath()

    init {
        kotlinJsTest.doNotTrackStateCompat("Should always re-run for WASM")
    }

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean
    ): TCServiceMessagesTestExecutionSpec {
        val testRunnerFile = writeWasmUnitTestRunner(task.inputFileProperty.get().asFile)

        forkOptions.executable = d8Executable.absolutePath
        forkOptions.workingDir = testRunnerFile.parentFile

        val clientSettings = TCServiceMessagesClientSettings(
            task.name,
            testNameSuffix = task.targetName,
            prependSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm,
            ignoreOutOfRootNodes = true,
            escapeTCMessagesInLog = isTeamCity.isPresent
        )

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        val args = mutableListOf<String>()
        with(args) {
            addWasmExperimentalArguments()
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

    override fun getPath(): String = "${kotlinJsTest.path}:kotlinTestFrameworkStub"
}