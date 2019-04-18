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
import org.jetbrains.kotlin.gradle.tasks.KotlinTestTask
import org.jetbrains.kotlin.gradle.testing.IgnoredTestSuites
import org.jetbrains.kotlin.gradle.testing.TestsGrouping
import java.io.File

open class KotlinNodeJsTestTask : KotlinTestTask() {
    @Input
    var ignoredTestSuites: IgnoredTestSuites =
        IgnoredTestSuites.showWithContents

    @InputDirectory
    lateinit var nodeModulesDir: File

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

    override fun createTestExecutionSpec(): TCServiceMessagesTestExecutionSpec {
        val extendedForkOptions = DefaultProcessForkOptions(fileResolver)
        nodeJsProcessOptions.copyTo(extendedForkOptions)

        extendedForkOptions.environment.addPath("NODE_PATH", nodeModulesDir.canonicalPath)

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

        val clientSettings = when (testsGrouping) {
            TestsGrouping.none -> TCServiceMessagesClientSettings(rootNodeName = name, skipRoots = true)
            TestsGrouping.root -> TCServiceMessagesClientSettings(rootNodeName = name, nameOfRootSuiteToReplace = targetName)
            TestsGrouping.leaf -> TCServiceMessagesClientSettings(
                rootNodeName = name,
                skipRoots = true,
                nameOfLeafTestToAppend = targetName
            )
        }

        return TCServiceMessagesTestExecutionSpec(
            extendedForkOptions,
            nodeJsArgs +
                    testRuntimeNodeModules
                        .map { nodeModulesDir.resolve(it) }
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

private fun MutableMap<String, Any>.addPath(key: String, path: String) {
    val prev = get(key)
    if (prev == null) set(key, path)
    else set(key, prev as String + File.pathSeparator + path)
}