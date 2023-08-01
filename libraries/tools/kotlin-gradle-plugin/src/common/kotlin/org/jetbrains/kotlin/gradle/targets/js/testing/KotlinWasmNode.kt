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
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.addWasmExperimentalArguments
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.writeWasmUnitTestRunner
import org.jetbrains.kotlin.gradle.utils.doNotTrackStateCompat
import org.jetbrains.kotlin.gradle.utils.getValue
import java.nio.file.Path

internal class KotlinWasmNode(private val kotlinJsTest: KotlinJsTest) : KotlinJsTestFramework {
    override val settingsState: String = "KotlinWasmNode"
    @Transient
    override val compilation: KotlinJsCompilation = kotlinJsTest.compilation
    private val isTeamCity = compilation.target.project.providers.gradleProperty(TCServiceMessagesTestExecutor.TC_PROJECT_PROPERTY)

    @Transient
    private val target: KotlinJsIrTarget = compilation.target as KotlinJsIrTarget

    @Transient
    private val project: Project = target.project

    private val npmProjectDir by project.provider { compilation.npmProject.dir }

    private val wasmTargetType: KotlinWasmTargetType? = target.wasmTargetType

    override val workingDir: Path
        get() = if (wasmTargetType != KotlinWasmTargetType.WASI) {
            npmProjectDir.toPath()
        } else {
            kotlinJsTest.inputFileProperty.get().asFile.toPath().parent
        }

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
            addAll(nodeJsArgs)
            addWasmExperimentalArguments()
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

    override fun getPath(): String = "${kotlinJsTest.path}:kotlinTestFrameworkStub"
}