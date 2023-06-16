/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import org.gradle.api.logging.Logging
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.build.report.metrics.ValueType
import org.jetbrains.kotlin.build.report.statistics.CompileStatisticsData
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.build.report.statistics.formatSize
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.LinkedHashSet
import kotlin.system.measureTimeMillis

class BuildScanReporter(private val buildUuid: String) : BuildReportService {
    private val log = Logging.getLogger(this.javaClass)
    private val tags = LinkedHashSet<StatTag>()

    internal fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        parameters: BuildReportParameters,
        buildScan: BuildScanExtensionHolder?,
    ) {
        if (buildScan == null) return
        parameters.reportingSettings.buildScanReportSettings ?: return

        initBuildScanTags(buildScan, parameters.label)
        addBuildScanReport(buildOperationRecords, parameters, buildScan)
        parameters.additionalTags.forEach { buildScan.buildScan.tag(it.readableString) }
        addCollectedTags(buildScan)
    }

    private fun addBuildScanReport(
        buildOperationRecords: Collection<BuildOperationRecord>,
        parameters: BuildReportParameters,
        buildScanExtension: BuildScanExtensionHolder
    ) {
        val buildScanSettings = parameters.reportingSettings.buildScanReportSettings ?: return

        val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
            transformOperationRecordsToCompileStatisticsData(
                buildOperationRecords,
                parameters,
                onlyKotlinTask = true,
                buildUuid = buildUuid,
                metricsToShow = buildScanSettings.metrics
            )
        }
        log.debug("Collect data takes $collectDataDuration: $compileStatData")

        compileStatData.forEach {
            addBuildScanReport(it, buildScanSettings.customValueLimit, buildScanExtension)
        }
    }

    override fun onFinish(
        event: TaskFinishEvent,
        buildOperation: BuildOperationRecord,
        parameters: BuildReportParameters,
        buildScan: BuildScanExtensionHolder?
    ) {
        if (buildScan == null || parameters.reportingSettings.buildScanReportSettings == null) return

        if (GradleVersion.current().baseVersion != GradleVersion.version("8.0")) return

        val buildScanSettings = parameters.reportingSettings.buildScanReportSettings

        val (collectDataDuration, compileStatData) = measureTimeMillisWithResult {
            prepareData(
                event,
                parameters.projectName, buildUuid, parameters.label,
                parameters.kotlinVersion,
                buildOperation,
                metricsToShow = buildScanSettings.metrics
            )
        }
        log.debug("Collect data takes $collectDataDuration: $compileStatData")

        compileStatData?.also {
            addBuildScanReport(it, buildScanSettings.customValueLimit, buildScan)
        }
    }

    override fun close(
        buildOperationRecords: Collection<BuildOperationRecord>,
        failureMessages: List<String>,
        parameters: BuildReportParameters,
    ) {
        //Do nothing, call close with  buildScan
    }

    private fun addBuildScanReport(
        data: CompileStatisticsData,
        customValuesLimit: Int,
        buildScan: BuildScanExtensionHolder,
    ) {
        val elapsedTime = measureTimeMillis {
            tags.addAll(data.tags)
            if (customValues < customValuesLimit) {
                readableString(data).forEach {
                    if (customValues < customValuesLimit) {
                        addBuildScanValue(buildScan, data, it)
                    } else {
                        log.debug(
                            "Can't add any more custom values into build scan." +
                                    " Statistic data for ${data.taskName} was cut due to custom values limit."
                        )
                    }
                }
            } else {
                log.debug("Can't add any more custom values into build scan.")
            }
        }

        log.debug("Report statistic to build scan takes $elapsedTime ms")
    }

    private fun addBuildScanValue(
        buildScan: BuildScanExtensionHolder,
        data: CompileStatisticsData,
        customValue: String
    ) {
        buildScan.buildScan.value(data.taskName, customValue)
        customValues++
    }

    private fun readableString(data: CompileStatisticsData): List<String> {
        val readableString = StringBuilder()
        if (data.nonIncrementalAttributes.isEmpty()) {
            readableString.append("Incremental build; ")
            data.changes.joinTo(readableString, prefix = "Changes: [", postfix = "]; ") { it.substringAfterLast(File.separator) }
        } else {
            data.nonIncrementalAttributes.joinTo(
                readableString,
                prefix = "Non incremental build because: [",
                postfix = "]; "
            ) { it.readableString }
        }

        data.kotlinLanguageVersion?.also {
            readableString.append("Kotlin language version: $it; ")
        }

        val timeData =
            data.buildTimesMetrics.map { (key, value) -> "${key.readableString}: ${value}ms" } //sometimes it is better to have separate variable to be able debug
        val perfData = data.performanceMetrics.map { (key, value) ->
            when (key.type) {
                ValueType.BYTES -> "${key.readableString}: ${formatSize(value)}"
                ValueType.MILLISECONDS -> DATE_FORMATTER.format(value)
                else -> "${key.readableString}: $value"
            }
        }
        timeData.union(perfData).joinTo(readableString, ",", "Performance: [", "]")

        return splitStringIfNeed(readableString.toString(), BuildReportsService.CUSTOM_VALUE_LENGTH_LIMIT)
    }

    private fun splitStringIfNeed(str: String, lengthLimit: Int): List<String> {
        val splattedString = ArrayList<String>()
        var tempStr = str
        while (tempStr.length > lengthLimit) {
            val subSequence = tempStr.substring(lengthLimit)
            var index = subSequence.lastIndexOf(';')
            if (index == -1) {
                index = subSequence.lastIndexOf(',')
                if (index == -1) {
                    index = lengthLimit
                }
            }
            splattedString.add(tempStr.substring(index))
            tempStr = tempStr.substring(index)

        }
        splattedString.add(tempStr)
        return splattedString
    }

    internal fun initBuildScanTags(buildScan: BuildScanExtensionHolder, label: String?) {
        buildScan.buildScan.tag(buildUuid)
        label?.also {
            buildScan.buildScan.tag(it)
        }
    }

    private fun addCollectedTags(buildScan: BuildScanExtensionHolder) {
        replaceWithCombinedTag(
            StatTag.KOTLIN_1,
            StatTag.KOTLIN_2,
            StatTag.KOTLIN_1_AND_2
        )

        replaceWithCombinedTag(
            StatTag.INCREMENTAL,
            StatTag.NON_INCREMENTAL,
            StatTag.INCREMENTAL_AND_NON_INCREMENTAL
        )

        tags.forEach { buildScan.buildScan.tag(it.readableString) }
    }

    private fun replaceWithCombinedTag(firstTag: StatTag, secondTag: StatTag, combinedTag: StatTag) {
        val containsFirstTag = tags.remove(firstTag)
        val containsSecondTag = tags.remove(secondTag)
        when {
            containsFirstTag && containsSecondTag -> tags.add(combinedTag)
            containsFirstTag -> tags.add(firstTag)
            containsSecondTag -> tags.add(secondTag)
        }
    }

    companion object {
        private var customValues = 0 // doesn't need to be thread-safe
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    }
}
