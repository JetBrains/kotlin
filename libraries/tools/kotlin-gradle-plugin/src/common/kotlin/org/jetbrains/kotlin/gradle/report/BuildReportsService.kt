/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.build.report.statistics.BuildStartParameters
import org.jetbrains.kotlin.build.report.statistics.HttpReportService
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import java.io.File
import java.net.InetAddress
import java.util.*

//Because of https://github.com/gradle/gradle/issues/23359 gradle issue, two build services interaction is not reliable at the end of the build
//Switch back to proper BuildService as soon as this issue is fixed
class BuildReportsService {

    private val log = Logging.getLogger(this.javaClass)
    private val buildUuid = UUID.randomUUID().toString()

    //build scan report has different behaviour depends on Gradle version
    internal val buildScanReporter = BuildScanReporter(buildUuid)

    private val reporters: List<BuildReportService> =
        listOf(
            HttpReporter(buildUuid),
            buildScanReporter,
            FileReporter(buildUuid),
            ConsoleReporter(),
            SingleFileReporter(),
            TaskExecutedReporter()
        )

    init {
        log.info("Build report service is registered. Unique build id: $buildUuid")
    }

    fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters,
    ) {
        reporters.forEach {
            it.close(buildOperationRecords, failureMessages, parameters)
        }
    }

    fun onFinish(
        event: TaskFinishEvent, buildOperation: BuildOperationRecord,
        parameters: BuildReportParameters,
        buildScan: BuildScanExtensionHolder?
    ) {
        reporters.forEach {
            it.onFinish(event, buildOperation, parameters, buildScan)
        }
    }

    companion object {

        const val CUSTOM_VALUE_LENGTH_LIMIT = 100_000

        fun getStartParameters(project: Project) = project.gradle.startParameter.let {
            BuildStartParameters(
                tasks = it.taskRequests.flatMap { it.args },
                excludedTasks = it.excludedTaskNames,
                currentDir = it.currentDir.path,
                projectProperties = it.projectProperties.map { (key, value) -> "$key: $value" },
                systemProperties = it.systemPropertiesArgs.map { (key, value) -> "$key: $value" },
            )
        }

        val hostName: String? = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Exception) {
            //do nothing
            null
        }
    }

}

enum class TaskExecutionState {
    SKIPPED,
    FAILED,
    UNKNOWN,
    SUCCESS,
    FROM_CACHE,
    UP_TO_DATE
    ;
}

internal interface BuildReportService {
    fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters,
    )

    fun onFinish(
        event: TaskFinishEvent,
        buildOperation: BuildOperationRecord,
        parameters: BuildReportParameters,
        buildScan: BuildScanExtensionHolder?,
    )
}

data class BuildReportParameters(
    val startParameters: BuildStartParameters,
    val reportingSettings: ReportingSettings,
    val httpService: HttpReportService?,

    val projectDir: File,
    val label: String?,
    val projectName: String,
    val kotlinVersion: String,
    val additionalTags: Set<StatTag>
)