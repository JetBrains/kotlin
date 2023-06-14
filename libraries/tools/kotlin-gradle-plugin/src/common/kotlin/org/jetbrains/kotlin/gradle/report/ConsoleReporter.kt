/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logging
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord

class ConsoleReporter : BuildReportService {
    private val log = Logging.getLogger(this.javaClass)

    override fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters,
    ) {
        if (parameters.reportingSettings.experimentalTryK2ConsoleOutput) {
            reportTryK2ToConsole(buildOperationRecords)
        }
    }

    private fun reportTryK2ToConsole(
        buildOperationRecords: Collection<BuildOperationRecord>,
    ) {
        val tasksData = buildOperationRecords
            .filterIsInstance<TaskRecord>()
            .filter {
                // Filtering by only KGP tasks and by those that actually do compilation
                it.isFromKotlinPlugin && it.kotlinLanguageVersion != null
            }
        log.warn("##### 'kotlin.experimental.tryK2' results (Kotlin/Native not checked) #####")
        if (tasksData.isEmpty()) {
            log.warn("No Kotlin compilation tasks have been run")
            log.warn("#####")
        } else {
            val tasksCountWithKotlin2 = tasksData.count {
                it.kotlinLanguageVersion != null && it.kotlinLanguageVersion >= KotlinVersion.KOTLIN_2_0
            }
            val taskWithK2Percent = (tasksCountWithKotlin2 * 100) / tasksData.count()
            val statsData = tasksData.map { it.path to it.kotlinLanguageVersion?.version }
            statsData.forEach { record ->
                log.warn("${record.first}: ${record.second} language version")
            }
            log.warn(
                "##### $taskWithK2Percent% ($tasksCountWithKotlin2/${tasksData.count()}) tasks have been compiled with Kotlin 2.0 #####"
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