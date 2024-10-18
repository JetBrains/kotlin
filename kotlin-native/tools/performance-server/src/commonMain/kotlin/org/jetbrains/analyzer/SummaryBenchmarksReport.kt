/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.BenchmarksReport
import org.jetbrains.report.Compiler
import org.jetbrains.report.Environment
import org.jetbrains.report.MeanVariance
import org.jetbrains.report.MeanVarianceBenchmark
import kotlin.math.abs

typealias SummaryBenchmark = Pair<MeanVarianceBenchmark?, MeanVarianceBenchmark?>
typealias BenchmarksTable = Map<String, MeanVarianceBenchmark>
typealias ScoreChange = Pair<MeanVariance, MeanVariance>

class DetailedBenchmarksReport(currentBenchmarks: Map<String, List<BenchmarkResult>>,
                               previousBenchmarks: Map<String, List<BenchmarkResult>>? = null,
                               val meaningfulChangesValue: Double = 0.5) {
    // Report created by joining comparing reports.
    val mergedReport: Map<String, SummaryBenchmark>

    // Maps with changes of performance.
    var regressions = mapOf<String, ScoreChange>()
        private set
    var improvements = mapOf<String, ScoreChange>()
        private set

    // Summary value of report - geometric mean.
    val geoMeanBenchmark: SummaryBenchmark
    var geoMeanScoreChange: ScoreChange? = null
        private set

    init {
        // Count avarage values for each benchmark.
        val currentBenchmarksTable = collectMeanResults(currentBenchmarks)
        val previousBenchmarksTable = previousBenchmarks?.let {
            collectMeanResults(previousBenchmarks)
        }
        mergedReport = createMergedReport(currentBenchmarksTable, previousBenchmarksTable)
        geoMeanBenchmark = calculateGeoMeanBenchmark(currentBenchmarksTable, previousBenchmarksTable)

        if (previousBenchmarks != null) {
            // Check changes in environment and tools.
            analyzePerformanceChanges()
        }
    }
    // Analyze and collect changes in performance between same becnhmarks.
    private fun analyzePerformanceChanges() {
        val performanceChanges = mergedReport.asSequence().map { (name, element) ->
            getBenchmarkPerfomanceChange(name, element)
        }.filterNotNull().groupBy {
            if (it.second.first.mean > 0) "regressions" else "improvements"
        }

        // Sort regressions and improvements.
        regressions = performanceChanges["regressions"]
                ?.sortedByDescending { it.second.first.mean }?.map { it.first to it.second }
                ?.toMap() ?: mapOf<String, ScoreChange>()
        improvements = performanceChanges["improvements"]
                ?.sortedBy { it.second.first.mean }?.map { it.first to it.second }
                ?.toMap() ?: mapOf<String, ScoreChange>()

        // Calculate change for geometric mean.
        val (current, previous) = geoMeanBenchmark
        geoMeanScoreChange = current?.let {
            previous?.let {
                Pair(current.calcPercentageDiff(previous), current.calcRatio(previous))
            }
        }
    }

    // Merge current and compare to report.
    private fun createMergedReport(currentBenchmarks: BenchmarksTable, previousBenchmarks: BenchmarksTable?):
            Map<String, SummaryBenchmark> {
        val mergedTable = mutableMapOf<String, SummaryBenchmark>()
        mergedTable.apply {
            currentBenchmarks.forEach { (name, current) ->
                // Check existance of benchmark in previous results.
                if (previousBenchmarks == null || name !in previousBenchmarks) {
                    getOrPut(name) { SummaryBenchmark(current, null) }
                } else {
                    getOrPut(name) { SummaryBenchmark(current, previousBenchmarks[name]) }
                }
            }
        }

        // Add removed benchmarks to merged report.
        mergedTable.apply {
            previousBenchmarks?.filter { (key, _) -> key !in currentBenchmarks }?.forEach { (key, value) ->
                getOrPut(key) { SummaryBenchmark(null, value) }
            }
        }

        return mergedTable
    }

    // Calculate geometric mean.
    private fun calculateGeoMeanBenchmark(currentBenchmarks: BenchmarksTable, previousBenchmarks: BenchmarksTable?):
            SummaryBenchmark {
        // Calculate geometric mean.
        val currentGeoMean = createGeoMeanBenchmark(currentBenchmarks)
        val previousGeoMean = previousBenchmarks?.let { createGeoMeanBenchmark(previousBenchmarks) }
        return SummaryBenchmark(currentGeoMean, previousGeoMean)
    }

    private fun getBenchmarkPerfomanceChange(name: String, benchmark: SummaryBenchmark): Pair<String, ScoreChange>? {
        val (current, previous) = benchmark
        current?.let {
            previous?.let {
                // Calculate metrics for showing difference.
                val percent = current.calcPercentageDiff(previous)
                val ratio = current.calcRatio(previous)
                if (abs(percent.mean) - percent.variance >= meaningfulChangesValue) {
                    return Pair(name, Pair(percent, ratio))
                }
            }
        }
        return null
    }

    // Create geometric mean.
    private fun createGeoMeanBenchmark(benchTable: BenchmarksTable): MeanVarianceBenchmark {
        val geoMeanBenchmarkName = "Geometric mean"
        val geoMean = geometricMean(benchTable.toList().map { (_, value) -> value.score })
        val varianceGeoMean = geometricMean(benchTable.toList().map { (_, value) -> value.variance })
        return MeanVarianceBenchmark(geoMeanBenchmarkName, geoMean, varianceGeoMean)
    }
}

// Summary report with comparasion of separate benchmarks results.
class SummaryBenchmarksReport(private val currentReport: BenchmarksReport,
                              private val previousReport: BenchmarksReport? = null,
                              private val meaningfulChangesValue: Double = 0.5) {

    // Count avarage values for each benchmark.
    private val detailedMetricReports: Map<BenchmarkResult.Metric, DetailedBenchmarksReport> = BenchmarkResult.Metric.entries.associateWith { metric ->
        val currentBenchmarks = currentReport.benchmarks.map { (name, benchmarks) ->
            name to benchmarks.filter { it.metric == metric }
        }.filter { it.second.isNotEmpty() }.toMap()
        val previousBenchmarks = previousReport?.benchmarks?.map { (name, benchmarks) ->
            name to benchmarks.filter { it.metric == metric }
        }?.filter { it.second.isNotEmpty() }?.toMap()
        DetailedBenchmarksReport(
                currentBenchmarks,
                previousBenchmarks,
                meaningfulChangesValue
        )
    }

    private val benchmarksDurations: Map<String, Pair<Double?, Double?>>

    // Environment and tools.
    private val environments: Pair<Environment, Environment?>
    private val compilers: Pair<Compiler, Compiler?>

    private fun <T> getReducedResult(convertor: (DetailedBenchmarksReport) -> List<T>): List<T> {
        return detailedMetricReports.values.map {
            convertor(it)
        }.flatten()
    }

    init {
        benchmarksDurations = calculateBenchmarksDuration(currentReport, previousReport)
        environments = Pair(currentReport.env, previousReport?.env)
        compilers = Pair(currentReport.compiler, previousReport?.compiler)
    }

    // Get benchmark report.
    fun getBenchmarksReport(takeMainReport: Boolean = true) =
            if (takeMainReport)
                BenchmarksReport(environments.first, getReducedResult { report ->
                    report.mergedReport.map { (_, value) -> value.first!! }
                }, compilers.first)
            else
                BenchmarksReport(environments.second!!, getReducedResult { report ->
                    report.mergedReport.map { (_, value) -> value.second!! }
                }, compilers.second!!)

    // Generate map with summary durations of each benchmark.
    private fun calculateBenchmarksDuration(currentReport: BenchmarksReport, previousReport: BenchmarksReport?):
            Map<String, Pair<Double?, Double?>> {
        val currentDurations = collectBenchmarksDurations(currentReport.benchmarks)
        val previousDurations = previousReport?.let {
            collectBenchmarksDurations(previousReport.benchmarks)
        } ?: mapOf()
        return currentDurations.keys.union(previousDurations.keys).associateWith { Pair(currentDurations[it], previousDurations[it]) }
    }

}