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

enum class Status {
    FAILED, FIXED, IMPROVED, REGRESSED, STABLE, UNSTABLE
}

// Report render to short summary statistics.
class StatisticsRender: Render() {
    override val name: String
        get() = "statistics"

    private var content = StringBuilder()

    override fun render(report: SummaryBenchmarksReport, onlyChanges: Boolean): String {
        val benchmarksWithChangedStatus = report.getBenchmarksWithChangedStatus()
        val newPasses = benchmarksWithChangedStatus
                .filter { it.current == BenchmarkResult.Status.PASSED }
        val newFailures = benchmarksWithChangedStatus
                .filter { it.current == BenchmarkResult.Status.FAILED }
        if (report.failedBenchmarks.isNotEmpty()) {
            content.append("failed: ${report.failedBenchmarks.size}\n")
        }
        val status = when {
            newFailures.isNotEmpty() -> {
                content.append("new failures: ${newFailures.size}\n")
                Status.FAILED
            }
            newPasses.isNotEmpty() -> {
                content.append("new passes: ${newPasses.size}\n")
                Status.FIXED
            }
            report.improvements.isNotEmpty() && report.regressions.isNotEmpty() -> {
                content.append("regressions: ${report.regressions.size}\nimprovements: ${report.improvements.size}")
                Status.UNSTABLE
            }
            report.improvements.isNotEmpty() && report.regressions.isEmpty() ->  {
                content.append("improvements: ${report.improvements.size}")
                Status.IMPROVED
            }
            report.improvements.isEmpty() && report.regressions.isNotEmpty() -> {
                content.append("regressions: ${report.regressions.size}")
                Status.REGRESSED
            }
            else -> Status.STABLE
        }
        return """
            status: $status
            total: ${report.benchmarksNumber}
        """.trimIndent() + "\n$content"
    }
}