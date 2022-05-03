/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.mocha

import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import java.io.File
import org.jetbrains.kotlin.gradle.targets.js.isTeamCity

class KotlinMocha(@Transient override val compilation: KotlinJsCompilation, private val basePath: String) :
    KotlinJsTestFramework {
    @Transient
    private val project: Project = compilation.target.project
    private val npmProject = compilation.npmProject
    private val versions = NodeJsRootPlugin.apply(project.rootProject).versions

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
            escapeTCMessagesInLog = project.isTeamCity
        )

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )

        val mocha = npmProject.require("mocha/bin/mocha")

        val file = task.inputFileProperty.get().asFile.toString()

        val adapter = createAdapterJs(file, "kotlin-test-nodejs-runner", ADAPTER_NODEJS)
        val args = mutableListOf(
            "--require",
            npmProject.require("source-map-support/register.js")
        ).apply {
            if (debug) {
                add("--inspect-brk")
            }
            add(mocha)
            add(adapter.canonicalPath)
            addAll(cliArgs.toList())
            addAll(cliArg("--reporter", "kotlin-test-js-runner/mocha-kotlin-reporter.js"))
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
                add(createAdapterJs(file, "kotlin-test-nodejs-empty-runner", ADAPTER_EMPTY_NODEJS).canonicalPath)
                addAll(cliArgs.toList())

                addAll(cliArg("-n", "experimental-wasm-typed-funcref,experimental-wasm-gc,experimental-wasm-eh"))
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

    private fun createAdapterJs(
        file: String,
        adapter: String,
        adapterName: String
    ): File {
        val adapterJs = npmProject.dir.resolve(adapterName)
        adapterJs.printWriter().use { writer ->
            val adapterFile = npmProject.require("kotlin-test-js-runner/$adapter.js")
            val escapedFile = file.jsQuoted()

            writer.println("require(${adapterFile.jsQuoted()})")

            writer.println("module.exports = require($escapedFile)")
        }

        return adapterJs
    }

    companion object {
        const val ADAPTER_NODEJS = "adapter-nodejs.js"
        const val ADAPTER_EMPTY_NODEJS = "adapter-empty-nodejs.js"

        private const val DEFAULT_TIMEOUT = "2s"
    }
}

private const val TIMEOUT_ARG = "--timeout"
private const val NO_TIMEOUT_ARG = "--no-timeout"