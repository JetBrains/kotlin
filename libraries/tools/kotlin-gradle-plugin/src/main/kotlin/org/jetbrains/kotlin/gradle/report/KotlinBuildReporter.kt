/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.intellij.util.text.DateFormatUtil.formatTime
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskState
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.state.TaskExecutionResults
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.utils.isConfigurationCacheAvailable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

internal fun configureBuildReporter(gradle: Gradle, log: Logger) {
    val rootProject = gradle.rootProject
    val properties = PropertiesProvider(rootProject)

    if (!properties.buildReportEnabled) return

    val perfLogDir = properties.buildReportDir
        ?: rootProject.buildDir.resolve("reports/kotlin-build").apply { mkdirs() }

    if (perfLogDir.isFile) {
        log.error("Kotlin build report cannot be created: '$perfLogDir' is a file")
        return
    }

    perfLogDir.mkdirs()
    val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)

    val perfReportFile = perfLogDir.resolve("${gradle.rootProject.name}-build-$ts.txt")

    if (!isConfigurationCacheAvailable(gradle)) {
        val reporter = KotlinBuildReporter(gradle, perfReportFile)
        gradle.addBuildListener(reporter)
    }

    val buildReportMode = if (properties.buildReportVerbose) BuildReportMode.VERBOSE else BuildReportMode.SIMPLE

    gradle.taskGraph.whenReady { graph ->
        graph.allTasks.asSequence()
            .filterIsInstance<AbstractKotlinCompile<*>>()
            .forEach { it.buildReportMode = buildReportMode }
    }

    log.kotlinDebug { "Configured Kotlin build reporter" }
}

internal class KotlinBuildReporter(
    private val gradle: Gradle,
    private val perfReportFile: File
) : BuildAdapter(), TaskExecutionListener {
    init {
        val dir = perfReportFile.parentFile
        check(dir.isDirectory) { "$dir does not exist or is a file" }
        check(!perfReportFile.isFile) { "Build report log file $perfReportFile exists already" }
    }

    private val taskStartNs = HashMap<Task, Long>()
    private val kotlinTaskTimeNs = HashMap<Task, Long>()
    private val tasksSb = StringBuilder()

    @Volatile
    private var allTasksTimeNs: Long = 0L

    @Synchronized
    override fun beforeExecute(task: Task) {
        taskStartNs[task] = System.nanoTime()
    }

    @Synchronized
    override fun afterExecute(task: Task, state: TaskState) {
        val startNs = taskStartNs[task] ?: return

        val endNs = System.nanoTime()
        val timeNs = endNs - startNs
        allTasksTimeNs += timeNs

        if (!task.javaClass.name.startsWith("org.jetbrains.kotlin")) {
            return
        }

        kotlinTaskTimeNs[task] = timeNs
        tasksSb.appendln()

        val skipMessage = state.skipMessage
        if (skipMessage != null) {
            tasksSb.appendln("$task was skipped: $skipMessage")
        } else {
            tasksSb.appendln("$task finished in ${formatTime(timeNs)}")
        }

        val path = task.path
        val executionResult = TaskExecutionResults[path]
        if (executionResult != null) {
            tasksSb.appendln("Execution strategy: ${executionResult.executionStrategy}")

            executionResult.icLogLines?.let { lines ->
                tasksSb.appendln("Compilation log for $task:")
                lines.forEach { tasksSb.appendln("  $it") }
            }
        }
    }

    @Synchronized
    override fun buildFinished(result: BuildResult) {
        KotlinBuildReporterHandler().buildFinished(gradle, perfReportFile, kotlinTaskTimeNs.mapKeys{it.key.path}, allTasksTimeNs, result.failure)
    }
}

