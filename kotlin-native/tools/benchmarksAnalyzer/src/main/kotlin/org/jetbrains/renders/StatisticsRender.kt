/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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