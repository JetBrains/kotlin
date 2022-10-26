/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.jetbrains.kotlin.gradle.BaseGradleIT
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.test.assertEquals
import kotlin.test.fail

private data class GradleExecutionOutputData(
    val konanArguments: Map<TaskPath, KonanArguments>,
    //val dirSnapshot: Map<FilePath, Hash>
) {
    @JvmInline
    value class TaskPath(val value: String)

    @JvmInline
    value class KonanArguments(val value: String)

//    @JvmInline
//    value class FilePath(val value: String)
//
//    @JvmInline
//    value class Hash(val value: String)
}

private fun assertExecutionOutput(before: GradleExecutionOutputData, after: GradleExecutionOutputData) {
    // Check konan arguments first
    for ((taskPath, taskKonanArgumentsBefore) in before.konanArguments) {
        val taskKonanArgumentsAfter = after.konanArguments[taskPath]

        assertEquals(
            taskKonanArgumentsBefore.value,
            taskKonanArgumentsAfter?.value,
            "Konan Arguments of task $taskPath doesn't match when Configuration Cache is reused"
        )
    }

    val newKonanArgumentsFound = after.konanArguments.keys - before.konanArguments.keys
    if (newKonanArgumentsFound.isNotEmpty()) {
        fail("These are new tasks where Konan arguments are found when Configuration Cache is reused: $newKonanArgumentsFound")
    }

    val missingKonanArguments = before.konanArguments.keys - after.konanArguments.keys
    if (missingKonanArguments.isNotEmpty()) {
        fail("These are missing tasks where Konan arguments are expected when Configuration Cache is reused: $missingKonanArguments")
    }
}

/**
 * Tests whether configuration cache for the tasks specified by [buildArguments] works on simple scenario when project is built twice non-incrementally.
 */
fun TestProject.assertSimpleConfigurationCacheScenarioWorks(
    vararg buildArguments: String,
    buildOptions: BuildOptions,
    executedTaskNames: List<String>? = null,
    checkUpToDateOnRebuild: Boolean = true,
    checkConfigurationCacheFileReport: Boolean = true,
) {
    // First, run a build that serializes the tasks state for configuration cache in further builds

    val executedTask: List<String> = executedTaskNames ?: buildArguments.toList()

    val executionOutputDataBefore: GradleExecutionOutputData
    build(*buildArguments, buildOptions = buildOptions) {
        assertTasksExecuted(*executedTask.toTypedArray())
        assertOutputContains(
            "Calculating task graph as no configuration cache is available for tasks: ${buildArguments.joinToString(separator = " ")}"
        )

        assertOutputContains("Configuration cache entry stored.")
        if (checkConfigurationCacheFileReport) assertConfigurationCacheReportNotCreated()
    }.also {
        executionOutputDataBefore = it.collectExecutionOutput()
    }

    build("clean", buildOptions = buildOptions)

    // Then run a build where tasks states are deserialized to check that they work correctly in this mode
    val executionOutputDataAfter: GradleExecutionOutputData
    build(*buildArguments, buildOptions = buildOptions) {
        assertTasksExecuted(*executedTask.toTypedArray())
        assertConfigurationCacheReused()
    }.also {
        executionOutputDataAfter = it.collectExecutionOutput()
    }

    assertExecutionOutput(executionOutputDataBefore, executionOutputDataAfter)

    if (checkUpToDateOnRebuild) {
        build(*buildArguments, buildOptions = buildOptions) {
            assertTasksUpToDate(*executedTask.toTypedArray())
        }
    }
}

private fun BuildResult.collectExecutionOutput() = GradleExecutionOutputData(
    konanArguments = tasks
        .associate { GradleExecutionOutputData.TaskPath(it.path) to extractKonanArguments(taskLog(it)) }
        .filterValuesNotNull(),
    //dirSnapshot = TODO()
)

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
private inline fun <K, V: Any> Map<K, V?>.filterValuesNotNull(): Map<K, V> =
    filterValues { it != null } as Map<K, V>

private fun BuildResult.taskLog(task: BuildTask): String {
    val startMarkerRegex = """\n> Task ${task.path}""".toRegex()
    val startMatch = startMarkerRegex.find(output) ?: return "<CANT FIND LOG START POINT OF TASK ${task.path}>"

    val endMarkerRegex = """
        ${task.path} \(Thread.+\) completed\. Took \S+ secs\.
    """.trimIndent().toRegex()
    val endMatch = endMarkerRegex.find(output) ?: return "<CANT FIND LOG END POINT OF TASK ${task.path}>"

    return output.substring(startMatch.range.first, endMatch.range.last)
}

private fun extractKonanArguments(taskLog: String): GradleExecutionOutputData.KonanArguments? {
    val after = taskLog.substringAfter("Transformed arguments = [", missingDelimiterValue = "")
    val log = after.substringBefore("\n]\n", missingDelimiterValue = "")

    return log.takeIf { it.isNotBlank() }?.let(GradleExecutionOutputData::KonanArguments)
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
        .takeIf { it.exists() }
        ?.findInPath("configuration-cache-report.html")
        ?.let { copyReportToTempDir(it) }

private val Path.asClickableFileUrl
    get() = URI("file", "", toUri().path, null, null).toString()

private fun GradleProject.assertConfigurationCacheReportNotCreated() {
    configurationCacheReportFile?.let { htmlReportFile ->
        fail("Configuration cache problems were found, check ${htmlReportFile.asClickableFileUrl} for details.")
    }
}

fun BuildResult.assertConfigurationCacheReused() {
    assertOutputContains("Reusing configuration cache.")
}

val BuildOptions.withConfigurationCache: BuildOptions
    get() = copy(configurationCache = true, configurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL)

fun main() {
    val log = File("/tmp/build.log").readText()
    val tasks = log.lines()
        .filter { it.startsWith("> Task ") }
        .map { it.removePrefix("> Task ").split(" ").first() }
        .distinct()

    val logs = tasks.associateWith { logs(log, it) }
    val konanArguments = logs
        .mapValues { extractKonanArguments(it.value) }
        .filterValues { it != null }

    println("Tasks: $tasks")
    println("Logs: $logs")
    println("Logs: $konanArguments")
}

private fun logs(output: String, path: String): String {
    val startMarkerRegex = """\n> Task ${path}""".toRegex()
    val startMatch = startMarkerRegex.find(output) ?: return "<CANT FIND LOG START POINT OF TASK ${path}>"

    val endMarkerRegex = """
        ${path} \(Thread.+\) completed\. Took \S+ secs\.
    """.trimIndent().toRegex()
    val endMatch = endMarkerRegex.find(output) ?: return "<CANT FIND LOG END POINT OF TASK ${path}>"

    return output.substring(startMatch.range.first, endMatch.range.last)
}