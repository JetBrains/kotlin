/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.BenchmarkResult
import org.jetbrains.report.MeanVarianceBenchmark

// Report render to text format.
class TeamCityStatisticsRender: Render() {
    override val name: String
        get() = "teamcity"

    private var content = StringBuilder()

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        val currentDurations = report.currentBenchmarksDuration

        content.append("##teamcity[testSuiteStarted name='Benchmarks']\n")

        // For current benchmarks print score as TeamCity Test Metadata
        report.currentMeanVarianceBenchmarks.forEach { benchmark ->
            renderBenchmark(benchmark, currentDurations[benchmark.name]!!)
            renderSummaryBecnhmarkValue(benchmark)
        }
        content.append("##teamcity[testSuiteFinished name='Benchmarks']\n")

        // Report geometric mean as build statistic value
        renderGeometricMean(report.geoMeanBenchmark.first!!)

        return content.toString()
    }

    private fun renderSummaryBecnhmarkValue(benchmark: MeanVarianceBenchmark) {
        content.append("##teamcity[testMetadata testName='${benchmark.name}' name='Mean'" +
                " type='number' value='${benchmark.score}']\n")
        content.append("##teamcity[testMetadata testName='${benchmark.name}' name='Variance'" +
                " type='number' value='${benchmark.variance}']\n")
    }

    // Produce benchmark as test in TeamCity
    private fun renderBenchmark(benchmark: BenchmarkResult , duration: Double) {
        content.append("##teamcity[testStarted name='${benchmark.name}']\n")
        if (benchmark.status == BenchmarkResult.Status.FAILED) {
            content.append("##teamcity[testFailed name='${benchmark.name}']\n")
        }
        // test_duration_in_milliseconds is set for TeamCity
        content.append("##teamcity[testFinished name='${benchmark.name}' duration='${(duration / 1000).toInt()}']\n")
    }

    private fun renderGeometricMean(geoMeanBenchmark: MeanVarianceBenchmark) {
        content.append("##teamcity[buildStatisticValue key='Geometric mean' value='${geoMeanBenchmark.score}']\n")
        content.append("##teamcity[buildStatisticValue key='Geometric mean variance' value='${geoMeanBenchmark.variance}']\n")
    }
}