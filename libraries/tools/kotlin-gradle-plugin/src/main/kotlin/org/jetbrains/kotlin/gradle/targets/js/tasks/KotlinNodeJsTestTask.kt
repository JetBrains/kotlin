/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.DefaultProcessForkOptions
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesClientSettings
import org.jetbrains.kotlin.gradle.internal.testing.TCServiceMessagesTestExecutionSpec
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTrace
import org.jetbrains.kotlin.gradle.targets.js.internal.parseNodeJsStackTraceAsJvm
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProjectLayout
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolver
import org.jetbrains.kotlin.gradle.tasks.KotlinTestTask
import org.jetbrains.kotlin.gradle.testing.IgnoredTestSuites
import java.io.File

open class KotlinNodeJsTestTask : KotlinTestTask() {
    @Input
    var ignoredTestSuites: IgnoredTestSuites =
        IgnoredTestSuites.showWithContents

    @Input
    @SkipWhenEmpty
    var nodeModulesToLoad: Set<String> = setOf()

    @Input
    lateinit var testRuntimeNodeModules: Collection<String>

    @Suppress("LeakingThis")
    @Internal
    val nodeJsProcessOptions: ProcessForkOptions = DefaultProcessForkOptions(fileResolver)

    @Suppress("unused")
    val nodeJsExecutable: String
        @Input get() = nodeJsProcessOptions.executable

    @Suppress("unused")
    val nodeJsWorkingDirCanonicalPath: String
        @Input get() = nodeJsProcessOptions.workingDir.canonicalPath

    @Input
    var debug: Boolean = false

    fun nodeJs(options: ProcessForkOptions.() -> Unit) {
        options(nodeJsProcessOptions)
    }

    override fun executeTests() {
        NpmResolver.resolve(project)
        super.executeTests()
    }

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val extendedForkOptions = DefaultProcessForkOptions(fileResolver)
        nodeJsProcessOptions.copyTo(extendedForkOptions)

        NpmResolver.resolve(project)

        val npmProjectLayout = NpmProjectLayout[project]
        extendedForkOptions.workingDir = npmProjectLayout.nodeWorkDir

        val nodeJsArgs = mutableListOf<String>()

        if (debug) {
            nodeJsArgs.add("--inspect-brk")
        }

        val cliArgs = KotlinNodeJsTestRunnerCliArgs(
            nodeModulesToLoad.toList(),
            filterExt.includePatterns + filterExt.commandLineIncludePatterns,
            excludes,
            ignoredTestSuites.cli
        )

        val clientSettings = TCServiceMessagesClientSettings(
            name,
            testNameSuffix = if (showTestTargetName) targetName else null,
            prepandSuiteName = true,
            stackTraceParser = ::parseNodeJsStackTraceAsJvm
        )

        return TCServiceMessagesTestExecutionSpec(
            extendedForkOptions,
            nodeJsArgs +
                    testRuntimeNodeModules
                        .map { npmProjectLayout.nodeModulesDir.resolve(it) }
                        .filter { it.exists() }
                        .map { it.absolutePath } +
                    cliArgs.toList(),
            true,
            clientSettings
        )
    }
}

data class KotlinNodeJsTestRunnerCliArgs(
    val moduleNames: List<String>,
    val include: Collection<String> = listOf(),
    val exclude: Collection<String> = listOf(),
    val ignoredTestSuites: IgnoredTestSuitesReporting = IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored
) {
    fun toList(): List<String> = mutableListOf<String>().also { args ->
        if (include.isNotEmpty()) {
            args.add("--include")
            args.add(include.joinToString(","))
        }

        if (exclude.isNotEmpty()) {
            args.add("--exclude")
            args.add(exclude.joinToString(","))
        }

        if (ignoredTestSuites !== IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored) {
            args.add("--ignoredTestSuites")
            args.add(ignoredTestSuites.name)
        }

        args.addAll(moduleNames)
    }

    @Suppress("EnumEntryName")
    enum class IgnoredTestSuitesReporting {
        skip, reportAsIgnoredTest, reportAllInnerTestsAsIgnored
    }
}