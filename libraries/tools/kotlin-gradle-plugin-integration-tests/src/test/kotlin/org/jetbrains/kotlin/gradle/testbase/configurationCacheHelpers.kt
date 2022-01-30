/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.gradle.BaseGradleIT
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.name
import kotlin.test.fail

/**
 * Tests whether configuration cache for the tasks specified by [buildArguments] works on simple scenario when project is built twice non-incrementally.
 */
fun TestProject.assertSimpleConfigurationCacheScenarioWorks(
    vararg buildArguments: String,
    buildOptions: BuildOptions,
    executedTaskNames: List<String>? = null,
    checkUpToDateOnRebuild: Boolean = true,
) {
    // First, run a build that serializes the tasks state for configuration cache in further builds

    val executedTask: List<String> = executedTaskNames ?: buildArguments.toList()

    build(*buildArguments, buildOptions = buildOptions) {
        assertTasksExecuted(*executedTask.toTypedArray())
        assertOutputContains(
            "Calculating task graph as no configuration cache is available for tasks: ${buildArguments.joinToString(separator = " ")}"
        )
        assertConfigurationCacheSucceeded()
    }

    build("clean", buildOptions = buildOptions)

    // Then run a build where tasks states are deserialized to check that they work correctly in this mode
    build(*buildArguments, buildOptions = buildOptions) {
        assertTasksExecuted(*executedTask.toTypedArray())
        assertOutputContains("Reusing configuration cache.")
    }

    if (checkUpToDateOnRebuild) {
        build(*buildArguments, buildOptions = buildOptions) {
            assertTasksUpToDate(*executedTask.toTypedArray())
        }
    }
}

/**
 * Copies all files from the directory containing the given [htmlReportFile] to a
 * fresh temp dir and returns a reference to the copied [htmlReportFile] in the new
 * directory.
 */
@OptIn(ExperimentalPathApi::class)
private fun copyReportToTempDir(htmlReportFile: Path): Path =
    createTempDirDeleteOnExit("report").let { tempDir ->
        htmlReportFile.parent.toFile().copyRecursively(tempDir.toFile())
        tempDir.resolve(htmlReportFile.name)
    }

/**
 * The configuration cache report file, if exists, indicates problems were
 * found while caching the task graph.
 */
private val GradleProject.configurationCacheReportFile
    get() = projectPath
        .resolve("build")
        .findInPath("configuration-cache-report.html")
        ?.let { copyReportToTempDir(it) }

private val Path.asClickableFileUrl
    get() = URI("file", "", toUri().path, null, null).toString()

private fun GradleProject.assertConfigurationCacheSucceeded() {
    configurationCacheReportFile?.let { htmlReportFile ->
        fail("Configuration cache problems were found, check ${htmlReportFile.asClickableFileUrl} for details.")
    }
}

val BuildOptions.withConfigurationCache: BuildOptions
    get() = copy(configurationCache = true, configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL)