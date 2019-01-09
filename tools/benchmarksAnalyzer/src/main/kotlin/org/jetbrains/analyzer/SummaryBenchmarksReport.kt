/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jetbrains.analyzer

import kotlin.math.abs
import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.Environment
import org.jetbrains.report.Compiler
import org.jetbrains.report.BenchmarksReport

typealias SummaryBenchmark = Pair<MeanVarianceBenchmark?, MeanVarianceBenchmark?>
typealias BenchmarksTable = Map<String, MeanVarianceBenchmark>
typealias SummaryBenchmarksTable = Map<String, SummaryBenchmark>
typealias ScoreChange = Pair<MeanVariance, MeanVariance>

// Summary report with comparasion of separate benchmarks results.
class SummaryBenchmarksReport (val currentReport: BenchmarksReport,
                               val previousReport: BenchmarksReport? = null,
                               val meaningfulChangesValue: Double = 0.5) {
    // Report created by joining comparing reports.
    val mergedReport: Map<String, SummaryBenchmark>
    val benchmarksDurations: Map<String, Pair<Double?, Double?>>

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

    // Changes in environment and tools.
    val envChanges: List<FieldChange<String>>
    val kotlinChanges: List<FieldChange<String>>

    // Countable properties.
    val failedBenchmarks: List<String>
        get() = mergedReport.filter { it.value.first?.meanBenchmark?.status == BenchmarkResult.Status.FAILED }
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
        get() = benchmarksDurations.filter{ it.value.first != null }.map { it.key to it.value.first!! }.toMap()

    init {
        // Count avarage values for each benchmark.
        val currentBenchmarksTable = collectMeanResults(currentReport.benchmarks)
        val previousBenchmarksTable = previousReport?.let {
            collectMeanResults(previousReport.benchmarks)
        }
        mergedReport = createMergedReport(currentBenchmarksTable, previousBenchmarksTable)
        benchmarksDurations = calculateBenchmarksDuration(currentReport, previousReport)
        geoMeanBenchmark = calculateGeoMeanBenchmark(currentBenchmarksTable, previousBenchmarksTable)
        if (previousReport != null) {
            // Check changes in environment and tools.
            envChanges = analyzeEnvChanges(currentReport.env, previousReport.env)
            kotlinChanges = analyzeKotlinChanges(currentReport.compiler, previousReport.compiler)
            analyzePerformanceChanges()
        } else {
            envChanges = listOf<FieldChange<String>>()
            kotlinChanges = listOf<FieldChange<String>>()
        }
    }

    fun getBenchmarksWithChangedStatus(): List<FieldChange<BenchmarkResult.Status>> = benchmarksWithChangedStatus

    // Create geometric mean.
    private fun createGeoMeanBenchmark(benchTable: BenchmarksTable): MeanVarianceBenchmark {
        val geoMeanBenchmarkName = "Geometric mean"
        val geoMean = geometricMean(benchTable.toList().map { (_, value) -> value.meanBenchmark.score })
        val varianceGeoMean = geometricMean(benchTable.toList().map { (_, value) -> value.varianceBenchmark.score })
        val meanBenchmark = BenchmarkResult(geoMeanBenchmarkName, geoMean)
        val varianceBenchmark = BenchmarkResult(geoMeanBenchmarkName, varianceGeoMean)
        return MeanVarianceBenchmark(meanBenchmark, varianceBenchmark)
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
                val currentBenchmark = current.meanBenchmark
                // Check existance of benchmark in previous results.
                if (previousBenchmarks == null || name !in previousBenchmarks) {
                    getOrPut(name) { SummaryBenchmark(current, null) }
                } else {
                    val previousBenchmark = previousBenchmarks[name]!!.meanBenchmark
                    getOrPut(name) { SummaryBenchmark(current, previousBenchmarks[name]) }
                    // Explore change of status.
                    if (previousBenchmark.status != currentBenchmark.status) {
                        val statusChange = FieldChange("$name", previousBenchmark.status, currentBenchmark.status)
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
        val previousGeoMean = previousBenchmarks?. let { createGeoMeanBenchmark(previousBenchmarks) }
        return SummaryBenchmark(currentGeoMean, previousGeoMean)
    }

    private fun getBenchmarkPerfomanceChange(name: String, benchmark: SummaryBenchmark): Pair<String, ScoreChange>? {
        val (current, previous) = benchmark
        current?.let {
            previous?.let {
                // Calculate metrics for showing difference.
                val percent = current.calcPercentageDiff(previous)
                val ratio = current.calcRatio(previous)
                if (abs(percent.mean) >= meaningfulChangesValue) {
                    return Pair(name, Pair(percent, ratio))
                }
            }
        }
        return null
    }

    // Analyze and collect changes in performance between same becnhmarks.
    private fun analyzePerformanceChanges() {
        val performanceChanges = mergedReport.asSequence().map {(name, element) ->
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
        geoMeanScoreChange = current?. let {
            previous?. let {
                Pair(current.calcPercentageDiff(previous), current.calcRatio(previous))
            }
        }
    }

    private fun <T> MutableList<FieldChange<T>>.addFieldChange(field: String, previous: T, current: T) {
        FieldChange.getFieldChangeOrNull(field, previous, current)?.let {
            add(it)
        }
    }

    private fun analyzeEnvChanges(currentEnv: Environment, previousEnv: Environment): List<FieldChange<String>> {
        return mutableListOf<FieldChange<String>>().apply {
            addFieldChange("Machine CPU", previousEnv.machine.cpu, currentEnv.machine.cpu)
            addFieldChange("Machine OS", previousEnv.machine.os, currentEnv.machine.os)
            addFieldChange("JDK version", previousEnv.jdk.version, currentEnv.jdk.version)
            addFieldChange("JDK vendor", previousEnv.jdk.vendor, currentEnv.jdk.vendor)
        }
    }

    private fun analyzeKotlinChanges(currentCompiler: Compiler, previousCompiler: Compiler): List<FieldChange<String>> {
        return mutableListOf<FieldChange<String>>().apply {
            addFieldChange("Backend type", previousCompiler.backend.type.type, currentCompiler.backend.type.type)
            addFieldChange("Backend version", previousCompiler.backend.version, currentCompiler.backend.version)
            addFieldChange("Backend flags", previousCompiler.backend.flags.toString(),
                    currentCompiler.backend.flags.toString())
            addFieldChange( "Kotlin version", previousCompiler.kotlinVersion, currentCompiler.kotlinVersion)
        }
    }
}