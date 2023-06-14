/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logging
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.build.report.statistics.file.FileReportService
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord

class FileReporter(private val buildUuid: String) : BuildReportService {
    private val log = Logging.getLogger(this.javaClass)
    private val loggerAdapter = GradleLoggerAdapter(log)

    override fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters,
    ) {
        parameters.reportingSettings.fileReportSettings?.also {
            FileReportService.reportBuildStatInFile(
                it.buildReportDir,
                parameters.projectName,
                it.includeMetricsInReport,
                transformOperationRecordsToCompileStatisticsData(
                    buildOperationRecords,
                    parameters,
                    buildUuid = buildUuid,
                    onlyKotlinTask = false
                ),
                parameters.startParameters,
                failureMessages.filter { it.isNotEmpty() },
                loggerAdapter
            )
        }
    }

    override fun onFinish(
        event: TaskFinishEvent,
        buildOperation: BuildOperationRecord,
        parameters: BuildReportParameters,
        buildScan: BuildScanExtensionHolder?
    ) {
        //Do nothing
    }
}