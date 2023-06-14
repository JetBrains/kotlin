/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logging
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord

class SingleFileReporter : BuildReportService {
    private val log = Logging.getLogger(this.javaClass)

    override fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters,
    ) {
        parameters.reportingSettings.singleOutputFile?.also { singleOutputFile ->
            MetricsWriter(singleOutputFile.absoluteFile).process(buildOperationRecords, failureMessages, log)
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