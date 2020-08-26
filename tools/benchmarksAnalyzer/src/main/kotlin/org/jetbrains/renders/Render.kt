/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.*

import kotlin.math.abs

// Base class for printing report in different formats.
abstract class Render {

    companion object {
        fun getRenderByName(name: String) =
            when (name) {
                "text" -> TextRender()
                "html" -> HTMLRender()
                "teamcity" -> TeamCityStatisticsRender()
                "statistics" -> StatisticsRender()
                "metrics" -> MetricResultsRender()
                else -> error("Unknown render $name")
            }
    }

    abstract val name: String

    abstract fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean = false): String

    // Print report using render.
    fun print(report: SummaryBenchmarksReport, onlyChanges: Boolean = false, outputFile: String? = null) {
        val content = render(report, onlyChanges)
        outputFile?.let {
            writeToFile(outputFile, content)
        } ?: println(content)
    }

    protected fun formatValue(number: Double, isPercent: Boolean = false): String =
            if (isPercent) number.format(2) + "%" else number.format()
}

// Report render to text format.
class TextRender: Render() {
    override val name: String
        get() = "text"

    private val content = StringBuilder()
    private val headerSeparator = "================="
    private val wideColumnWidth = 50
    private val standardColumnWidth = 25

    private fun append(text: String = "") {
        content.append("$text\n")
    }

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        renderEnvChanges(report.envChanges, "Environment")
        renderEnvChanges(report.kotlinChanges, "Compiler")
        renderStatusSummary(report)
        renderStatusChangesDetails(report.getBenchmarksWithChangedStatus())
        renderPerformanceSummary(report)
        renderPerformanceDetails(report, onlyChanges)
        return content.toString()
    }

    private fun printBucketInfo(bucket: Collection<Any>, name: String) {
        if (!bucket.isEmpty()) {
            append("$name: ${bucket.size}")
        }
    }

    private fun <T> printStatusChangeInfo(bucket: List<FieldChange<T>>, name: String) {
        if (!bucket.isEmpty()) {
            append("$name:")
            for (change in bucket) {
                append(change.renderAsText())
            }
        }
    }

    fun renderEnvChanges(envChanges: List<FieldChange<String>>, bucketName: String) {
        if (!envChanges.isEmpty()) {
            append(ChangeReport(bucketName, envChanges).renderAsTextReport())
        }
    }

    fun renderStatusChangesDetails(benchmarksWithChangedStatus: List<FieldChange<BenchmarkResult.Status>>) {
        if (!benchmarksWithChangedStatus.isEmpty()) {
            append("Changes in status")
            append(headerSeparator)
            printStatusChangeInfo(benchmarksWithChangedStatus
                    .filter { it.current == BenchmarkResult.Status.FAILED }, "New failures")
            printStatusChangeInfo(benchmarksWithChangedStatus
                    .filter { it.current == BenchmarkResult.Status.PASSED }, "New passes")
            append()
        }
    }

    fun renderStatusSummary(report: SummaryBenchmarksReport) {
        append("Status summary")
        append(headerSeparator)

        val failedBenchmarks = report.failedBenchmarks
        val addedBenchmarks = report.addedBenchmarks
        val removedBenchmarks = report.removedBenchmarks
        if (failedBenchmarks.isEmpty()) {
            append("All benchmarks passed!")
        }
        if (!failedBenchmarks.isEmpty() || !addedBenchmarks.isEmpty() || !removedBenchmarks.isEmpty()) {
            printBucketInfo(failedBenchmarks, "Failed benchmarks")
            printBucketInfo(addedBenchmarks, "Added benchmarks")
            printBucketInfo(removedBenchmarks, "Removed benchmarks")
        }
        append("Total becnhmarks number: ${report.benchmarksNumber}")
        append()
    }

    fun renderPerformanceSummary(report: SummaryBenchmarksReport) {
        if (!report.regressions.isEmpty() || !report.improvements.isEmpty()) {
            append("Performance summary")
            append(headerSeparator)
            if (!report.regressions.isEmpty()) {
                append("Regressions: Maximum = ${formatValue(report.maximumRegression, true)}," +
                        " Geometric mean = ${formatValue(report.regressionsGeometricMean, true)}")
            }
            if (!report.improvements.isEmpty()) {
                append("Improvements: Maximum = ${formatValue(report.maximumImprovement, true)}," +
                        " Geometric mean = ${formatValue(report.improvementsGeometricMean, true)}")
            }
            append()
        }
    }

    private fun formatColumn(content:String, isWide: Boolean = false): String =
            content.padEnd(if (isWide) wideColumnWidth else standardColumnWidth, ' ')

    private fun printBenchmarksDetails(fullSet: Map<String, SummaryBenchmark>,
                                       bucket: Map<String, ScoreChange>? = null) {
        val placeholder = "-"
        if (bucket != null) {
            // There are changes in performance.
            // Output changed benchmarks.
            for ((name, change) in bucket) {
                append(formatColumn(name, true) +
                        formatColumn(fullSet.getValue(name).first?.description ?: placeholder) +
                        formatColumn(fullSet.getValue(name).second?.description ?: placeholder) +
                        formatColumn(change.first.description + " %") +
                        formatColumn(change.second.description))
            }
        } else {
            // Output all values without performance changes.
            for ((name, value) in fullSet) {
                append(formatColumn(name, true) +
                        formatColumn(value.first?.description ?: placeholder) +
                        formatColumn(value.second?.description ?: placeholder) +
                        formatColumn(placeholder) +
                        formatColumn(placeholder))
            }
        }
    }

    private fun printTableLineSeparator(tableWidth: Int) =
            append("${"-".padEnd(tableWidth, '-')}")

    private fun printPerformanceTableHeader(): Int {
        val wideColumns = listOf(formatColumn("Benchmark", true))
        val standardColumns = listOf(formatColumn("First score"),
                formatColumn("Second score"),
                formatColumn("Percent"),
                formatColumn("Ratio"))
        val tableWidth = wideColumnWidth * wideColumns.size + standardColumnWidth * standardColumns.size
        append("${wideColumns.joinToString(separator = "")}${standardColumns.joinToString(separator = "")}")
        printTableLineSeparator(tableWidth)
        return tableWidth
    }

    fun renderPerformanceDetails(report: SummaryBenchmarksReport, onlyChanges: Boolean = false) {
        append("Performance details")
        append(headerSeparator)

        if (onlyChanges) {
            if (report.regressions.isEmpty() && report.improvements.isEmpty()) {
                append("All becnhmarks are stable.")
            }
        }

        val tableWidth = printPerformanceTableHeader()
        // Print geometric mean.
        val geoMeanChangeMap = report.geoMeanScoreChange?.
                let { mapOf(report.geoMeanBenchmark.first!!.name to report.geoMeanScoreChange!!) }
        printBenchmarksDetails(
                mutableMapOf(report.geoMeanBenchmark.first!!.name to report.geoMeanBenchmark),
                geoMeanChangeMap)
        printTableLineSeparator(tableWidth)
        printBenchmarksDetails(report.mergedReport, report.regressions)
        printBenchmarksDetails(report.mergedReport, report.improvements)
        if (!onlyChanges) {
            // Print all remaining results.
            printBenchmarksDetails(report.mergedReport.filter { it.key !in report.regressions.keys &&
                    it.key !in report.improvements.keys })
        }
    }
}