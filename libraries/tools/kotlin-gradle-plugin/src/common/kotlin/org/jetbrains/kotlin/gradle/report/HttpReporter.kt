/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logging
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.build.report.statistics.BuildFinishStatisticsData
import org.jetbrains.kotlin.build.report.statistics.BuildStartParameters
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class HttpReporter(private val buildUuid: String) : BuildReportService {

    private val startTime = System.nanoTime()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()

    private val log = Logging.getLogger(this.javaClass)
    private val loggerAdapter = GradleLoggerAdapter(log)
    private val tags = HashSet<StatTag>()

    override fun onFinish(
        event: TaskFinishEvent,
        buildOperation: BuildOperationRecord,
        parameters: BuildReportParameters,
        buildScan: BuildScanExtensionHolder?,
    ) {
        parameters.reportingSettings.httpReportSettings ?: return

        val data =
            prepareData(
                event,
                parameters.projectName,
                buildUuid,
                parameters.label,
                parameters.kotlinVersion,
                buildOperation,
                onlyKotlinTask = true,
                parameters.additionalTags
            )
        data?.also {
            executorService.submit {
                parameters.httpService?.sendData(data, loggerAdapter)
            }
            tags.addAll(it.tags)
        }
    }

    override fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters,
    ) {
        executorService.submit { reportBuildFinish(parameters, tags) }
        //It's expected that bad internet connection can cause a significant delay for big project
        executorService.shutdown()
    }

    private fun reportBuildFinish(parameters: BuildReportParameters, tags: Set<StatTag>) {
        val httpReportSettings = parameters.reportingSettings.httpReportSettings ?: return

        val branchName = if (httpReportSettings.includeGitBranchName) {
            val process = ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD")
                .directory(parameters.projectDir)
                .start().also {
                    it.waitFor(5, TimeUnit.SECONDS)
                }
            process.inputStream.reader().readText()
        } else "is not set"

        val buildFinishData = BuildFinishStatisticsData(
            projectName = parameters.projectName,
            startParameters = parameters.startParameters
                .includeVerboseEnvironment(parameters.reportingSettings.httpReportSettings.verboseEnvironment),
            buildUuid = buildUuid,
            label = parameters.label,
            totalTime = TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - startTime)),
            finishTime = System.currentTimeMillis(),
            hostName = BuildReportsService.hostName,
            tags = tags,
            gitBranch = branchName
        )

        parameters.httpService?.sendData(buildFinishData, loggerAdapter)
    }

    private fun BuildStartParameters.includeVerboseEnvironment(verboseEnvironment: Boolean): BuildStartParameters {
        return if (verboseEnvironment) {
            this
        } else {
            BuildStartParameters(
                tasks = this.tasks,
                excludedTasks = this.excludedTasks,
                currentDir = null,
                projectProperties = emptyList(),
                systemProperties = emptyList()
            )
        }
    }
}