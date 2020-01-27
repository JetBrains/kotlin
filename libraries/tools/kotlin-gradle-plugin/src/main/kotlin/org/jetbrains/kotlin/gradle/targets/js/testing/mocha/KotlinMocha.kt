/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.mocha

import org.gradle.api.Project
import org.gradle.process.ProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutor.Companion.TC_PROJECT_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinGradleNpmPackage
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.jsQuoted
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTestFramework
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs
import java.io.File

class KotlinMocha(override val compilation: KotlinJsCompilation) :
    KotlinJsTestFramework {
    private val project: Project = compilation.target.project
    private val nodeJs = NodeJsRootPlugin.apply(project.rootProject)
    private val versions = nodeJs.versions

    override val settingsState: String
        get() = "mocha"

    override val requiredNpmDependencies: Collection<RequiredKotlinJsDependency>
        get() = listOf(
            KotlinGradleNpmPackage("test-js-runner"),
            versions.mocha,
            versions.sourceMapSupport
        )

    // https://mochajs.org/#-timeout-ms-t-ms
    var timeout: String = DEFAULT_TIMEOUT

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
            escapeTCMessagesInLog = project.hasProperty(TC_PROJECT_PROPERTY)
        )

        val npmProject = compilation.npmProject

        val cliArgs = KotlinTestRunnerCliArgs(
            include = task.includePatterns,
            exclude = task.excludePatterns
        )


        val mocha = npmProject.require("mocha/bin/mocha")

        val file = task.nodeModulesToLoad
            .map { npmProject.require(it) }
            .single()

        val adapter = createAdapterJs(file, debug)

        val args = mutableListOf(
            "--require",
            npmProject.require("source-map-support/register.js")
        ).apply {
            if (debug) {
                // Idle run of tests to load file with source maps to enable break points
                add("--require")
                add(
                    npmProject.require("kotlin-test-js-runner/kotlin-test-nodejs-idle-runner.js")
                )
                add("--require")
                add(file)

                add("--inspect-brk")
            }
            add(mocha)
            add(adapter.canonicalPath)
            addAll(cliArgs.toList())
            addAll(cliArg("--reporter", "kotlin-test-js-runner/mocha-kotlin-reporter.js"))
            addAll(cliArg("--timeout", timeout))
        }

        return TCServiceMessagesTestExecutionSpec(
            forkOptions,
            args,
            false,
            clientSettings
        )
    }

    private fun cliArg(cli: String, value: String?): List<String> {
        return value?.let { listOf(cli, it) } ?: emptyList()
    }

    private fun createAdapterJs(
        file: String,
        debug: Boolean
    ): File {
        val npmProject = compilation.npmProject

        val adapterJs = npmProject.dir.resolve(ADAPTER_NODEJS)
        adapterJs.printWriter().use { writer ->
            val adapter = npmProject.require("kotlin-test-js-runner/kotlin-test-nodejs-runner.js")
            val escapedFile = file.jsQuoted()

            if (debug) {
                // Invalidate caches after idle run
                writer.println("delete require.cache[require.resolve($escapedFile)]")
                writer.println("delete require.cache[require.resolve('kotlin-test')]")
            }

            writer.println("require(${adapter.jsQuoted()})")

            writer.println("module.exports = require($escapedFile)")
        }

        return adapterJs
    }

    companion object {
        const val ADAPTER_NODEJS = "adapter-nodejs.js"

        private const val DEFAULT_TIMEOUT = "2s"
    }
}