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
typealias SummaryBenchmarksTable = Map<String, SummaryBenchmark>
typealias ScoreChange = Pair<MeanVariance, MeanVariance>

// Summary report with comparasion of separate benchmarks results.
class SummaryBenchmarksReport(val currentReport: BenchmarksReport,
                              val previousReport: BenchmarksReport? = null,
                              val meaningfulChangesValue: Double = 0.5) {
    // Report created by joining comparing reports.
    val mergedReport: Map<String, SummaryBenchmark>
    private val benchmarksDurations: Map<String, Pair<Double?, Double?>>

    // Lists of benchmarks in different status.
    private val benchmarksWithChangedStatus = mutableListOf<FieldChange<BenchmarkResult.Status>>()

    // Maps with changes of performance.
    var regressions = mapOf<String, ScoreChange>()
        private set
    var improvements = mapOf<String, ScoreChange>()
        private set

    // Summary value of report - geometric mean.
    val geoMeanBenchmark: SummaryBenchmark
    var geoMeanScoreChange: ScoreChange? = null
        private set

    // Environment and tools.
    val environments: Pair<Environment, Environment?>
    val compilers: Pair<Compiler, Compiler?>

    // Countable properties.
    val failedBenchmarks: List<String>
        get() = mergedReport.filter { it.value.first?.status == BenchmarkResult.Status.FAILED }
                .map { it.key }

    val addedBenchmarks: List<String>
        get() = mergedReport.filter { it.value.second == null }.map { it.key }

    val removedBenchmarks: List<String>
        get() = mergedReport.filter { it.value.first == null }.map { it.key }

    val benchmarksNumber: Int
        get() = mergedReport.keys.size

    val currentMeanVarianceBenchmarks: List<MeanVarianceBenchmark>
        get() = mergedReport.filter { it.value.first != null }.map { it.value.first!! }

    val currentBenchmarksDuration: Map<String, Double>
        get() = benchmarksDurations.filter { it.value.first != null }.map { it.key to it.value.first!! }.toMap()

    val maximumRegression: Double
        get() = getMaximumChange(regressions)

    val maximumImprovement: Double
        get() = getMaximumChange(improvements)

    val regressionsGeometricMean: Double
        get() = getGeometricMeanOfChanges(regressions)

    val improvementsGeometricMean: Double
        get() = getGeometricMeanOfChanges(improvements)

    val envChanges: List<FieldChange<String>>
        get() {
            val previousEnvironment = environments.second
            val currentEnvironment = environments.first
            return previousEnvironment?.let {
                mutableListOf<FieldChange<String>>().apply {
                    addFieldChange("Machine CPU", previousEnvironment.machine.cpu, currentEnvironment.machine.cpu)
                    addFieldChange("Machine OS", previousEnvironment.machine.os, currentEnvironment.machine.os)
                    addFieldChange("JDK version", previousEnvironment.jdk.version, currentEnvironment.jdk.version)
                    addFieldChange("JDK vendor", previousEnvironment.jdk.vendor, currentEnvironment.jdk.vendor)
                }
            } ?: listOf<FieldChange<String>>()
        }

    val kotlinChanges: List<FieldChange<String>>
        get() {
            val previousCompiler = compilers.second
            val currentCompiler = compilers.first
            return previousCompiler?.let {
                mutableListOf<FieldChange<String>>().apply {
                    addFieldChange("Backend type", previousCompiler.backend.type.type, currentCompiler.backend.type.type)
                    addFieldChange("Backend version", previousCompiler.backend.version, currentCompiler.backend.version)
                    addFieldChange("Backend flags", previousCompiler.backend.flags.toString(),
                            currentCompiler.backend.flags.toString())
                    addFieldChange("Kotlin version", previousCompiler.kotlinVersion, currentCompiler.kotlinVersion)
                }
            } ?: listOf<FieldChange<String>>()
        }

    init {
        // Count avarage values for each benchmark.
        val currentBenchmarksTable = collectMeanResults(currentReport.benchmarks)
        val previousBenchmarksTable = previousReport?.let {
            collectMeanResults(previousReport.benchmarks)
        }
        mergedReport = createMergedReport(currentBenchmarksTable, previousBenchmarksTable)
        benchmarksDurations = calculateBenchmarksDuration(currentReport, previousReport)
        geoMeanBenchmark = calculateGeoMeanBenchmark(currentBenchmarksTable, previousBenchmarksTable)
        environments = Pair(currentReport.env, previousReport?.env)
        compilers = Pair(currentReport.compiler, previousReport?.compiler)

        if (previousReport != null) {
            // Check changes in environment and tools.
            analyzePerformanceChanges()
        }
    }

    // Get benchmark report.
    fun getBenchmarksReport(takeMainReport: Boolean = true) =
            if (takeMainReport)
                BenchmarksReport(environments.first, mergedReport.map { (_, value) -> value.first!! }, compilers.first)
            else
                BenchmarksReport(environments.second!!, mergedReport.map { (_, value) -> value.second!! }, compilers.second!!)

    fun getResultsByMetric(metric: BenchmarkResult.Metric, getGeoMean: Boolean = true, filter: List<String>? = null,
                           normalizeData: Map<String, Map<String, Double>>? = null): List<Double?> {
        val benchmarks = filter?.let {
            mergedReport.filter { entry ->
                filter.find {
                    entry.key.startsWith(it)
                } != null
            }
        } ?: mergedReport
        val results = benchmarks.map { entry ->
            val name = entry.key.removeSuffix(metric.suffix)
            if (entry.value.first!!.metric == metric) {
                val score = entry.value.first!!.score
                val value = normalizeData?.let {
                    it.get(name)?.get("$metric")?.let { score / it }
                            ?: error("No normalization data for benchmark $name and metric $metric")
                } ?: score
                name to value
            } else name to null
        }.toMap()
        if (getGeoMean) {
            return listOf(geometricMean(results.values.filterNotNull()))
        }
        return filter?.let { it.map { results[it] }.toList() } ?: results.values.toList()
    }

    private fun getMaximumChange(bucket: Map<String, ScoreChange>): Double =
            // Maps of regressions and improvements are sorted.
            if (bucket.isEmpty()) 0.0 else bucket.values.map { it.first.mean }.first()

    private fun getGeometricMeanOfChanges(bucket: Map<String, ScoreChange>): Double {
        if (bucket.isEmpty())
            return 0.0
        var percentsList = bucket.values.map { it.first.mean }
        return if (percentsList.first() > 0.0) {
            geometricMean(percentsList, benchmarksNumber)
        } else {
            // Geometric mean can be counted on positive numbers.
            percentsList = percentsList.map { abs(it) }
            -geometricMean(percentsList, benchmarksNumber)
        }
    }

    fun getBenchmarksWithChangedStatus(): List<FieldChange<BenchmarkResult.Status>> = benchmarksWithChangedStatus

    // Create geometric mean.
    private fun createGeoMeanBenchmark(benchTable: BenchmarksTable): MeanVarianceBenchmark {
        val geoMeanBenchmarkName = "Geometric mean"
        val geoMean = geometricMean(benchTable.toList().map { (_, value) -> value.score })
        val varianceGeoMean = geometricMean(benchTable.toList().map { (_, value) -> value.variance })
        return MeanVarianceBenchmark(geoMeanBenchmarkName, geoMean, varianceGeoMean)
    }

    // Generate map with summary durations of each benchmark.
    private fun calculateBenchmarksDuration(currentReport: BenchmarksReport, previousReport: BenchmarksReport?):
            Map<String, Pair<Double?, Double?>> {
        val currentDurations = collectBenchmarksDurations(currentReport.benchmarks)
        val previousDurations = previousReport?.let {
            collectBenchmarksDurations(previousReport.benchmarks)
        } ?: mapOf<String, Double>()
        return currentDurations.keys.union(previousDurations.keys)
                .map { it to Pair(currentDurations[it], previousDurations[it]) }.toMap()
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
                    val previousBenchmark = previousBenchmarks.getValue(name)
                    getOrPut(name) { SummaryBenchmark(current, previousBenchmarks[name]) }
                    // Explore change of status.
                    if (previousBenchmark.status != current.status) {
                        val statusChange = FieldChange("$name", previousBenchmark.status, current.status)
                        benchmarksWithChangedStatus.add(statusChange)
                    }
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

    private fun <T> MutableList<FieldChange<T>>.addFieldChange(field: String, previous: T, current: T) {
        FieldChange.getFieldChangeOrNull(field, previous, current)?.let {
            add(it)
        }
    }
}