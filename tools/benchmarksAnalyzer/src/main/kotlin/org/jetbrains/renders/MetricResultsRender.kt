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
class MetricResultsRender: Render() {
    override val name: String
        get() = "metrics"

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        val results = report.mergedReport.map { entry ->
            buildString {
                val metric = entry.value.first!!.meanBenchmark.metric
                append("{ \"benchmarkName\": \"${entry.key.removeSuffix(metric.suffix)}\",")
                append("\"metric\": \"${metric}\",")
                append("\"value\": \"${entry.value.first!!.meanBenchmark.score}\" }")
            }
        }.joinToString(", ")
        return "[ $results ]"

    }
}