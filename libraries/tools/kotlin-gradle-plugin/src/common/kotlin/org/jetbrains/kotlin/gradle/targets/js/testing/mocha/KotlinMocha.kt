/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.mocha

import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs

class KotlinMocha(@Transient override val compilation: KotlinJsCompilation, private val basePath: String) :
    KotlinJsTestFramework {
    @Transient
    private val project: Project = compilation.target.project
    private val npmProject = compilation.npmProject
    private val versions = NodeJsRootPlugin.apply(project.rootProject).versions
    private val isTeamCity = project.providers.gradleProperty(TCServiceMessagesTestExecutor.TC_PROJECT_PROPERTY)

    override val settingsState: String
        get() = "mocha"

    override val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
        get() = setOf(
            versions.kotlinJsTestRunner,
            versions.mocha,
            versions.sourceMapSupport,
            versions.formatUtil
        )

    override fun getPath() = "$basePath:kotlinMocha"

    // https://mochajs.org/#-timeout-ms-t-ms
    var timeout: String = DEFAULT_TIMEOUT

    private val platformType = compilation.platformType

    override fun createTestExecutionSpec(
        task: KotlinJsTest,
        forkOptions: ProcessForkOptions,
        nodeJsArgs: MutableList<String>,
        debug: Boolean
    ): TCServiceMessagesTestExecutionSpec {
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

        val mocha = npmProject.require("mocha/bin/mocha")

        val file = task.inputFileProperty.get().asFile.toString()

        val args = mutableListOf(
            "--require",
            npmProject.require("source-map-support/register.js")
        ).apply {
            if (debug) {
                add("--inspect-brk")
            }
            add(mocha)
            add(file)
            addAll(cliArgs.toList())
            addAll(cliArg("--reporter", "kotlin-test-js-runner/mocha-kotlin-reporter.js"))
            addAll(cliArg("--require", npmProject.require("kotlin-test-js-runner/kotlin-test-nodejs-runner.js")))
            if (debug) {
                add(NO_TIMEOUT_ARG)
            } else {
                addAll(cliArg(TIMEOUT_ARG, timeout))
            }
            if (platformType == KotlinPlatformType.wasm) {
                addAll(cliArg("-n", "experimental-wasm-typed-funcref,experimental-wasm-gc,experimental-wasm-eh"))
            }
        }

        val dryRunArgs = if (platformType == KotlinPlatformType.wasm)
            null
        else {
            mutableListOf(
                "--require",
                npmProject.require("source-map-support/register.js")
            ).apply {
                add(mocha)
                add(file)
                addAll(cliArgs.toList())
                addAll(cliArg("--require", npmProject.require("kotlin-test-js-runner/kotlin-test-nodejs-empty-runner.js")))
            }
        }

        return TCServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            false,
            clientSettings,
            dryRunArgs
        )
    }

    private fun cliArg(cli: String, value: String?): List<String> {
        return value?.let { listOf(cli, it) } ?: emptyList()
    }

    companion object {
        private const val DEFAULT_TIMEOUT = "2s"
    }
}

private const val TIMEOUT_ARG = "--timeout"
private const val NO_TIMEOUT_ARG = "--no-timeout"