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


package org.jetbrains.renders

import org.jetbrains.analyzer.*
import org.jetbrains.report.BenchmarkResult

// Report render to text format.
class TeamCityStatisticsRender: Render {
    private var content = StringBuilder()

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        // For current benchmarks print score as TeamCity Test Metadata
        report.currentMeanVarianceBenchmarks.forEach { benchmark ->
            content.append(renderBenchmark(benchmark.meanBenchmark, "Mean"))
            content.append(renderBenchmark(benchmark.varianceBenchmark, "Variance"))
        }
        // Report geometric mean as build statistic value
        content.append(renderGeometricMean(report.geoMeanBenchmark.first!!))
        return content.toString()
    }

    private fun renderBenchmark(benchmark: BenchmarkResult, metric: String): String =
        "##teamcity[testMetadata testName='${benchmark.name}' name='$metric' type='number' value='${benchmark.score}']\n"

    private fun renderGeometricMean(geoMeanBenchmark: MeanVarianceBenchmark): String =
        "##teamcity[buildStatisticValue key='Geometric mean' value='${geoMeanBenchmark.meanBenchmark.score}']\n" +
                "##teamcity[buildStatisticValue key='Geometric mean variance' value='${geoMeanBenchmark.varianceBenchmark.score}']\n"
}