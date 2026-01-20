/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cli.*
import org.jetbrains.analyzer.*
import org.jetbrains.renders.*
import org.jetbrains.report.*
import org.jetbrains.report.json.*

fun getBenchmarkReport(fileName: String): List<BenchmarksReport> {
    require(fileName.endsWith(".json")) {
        "Only json files are supported, but was given: $fileName"
    }
    val jsonEntity = JsonTreeParser.parse(readFile(fileName))
    return when (jsonEntity) {
        is JsonObject -> listOf(BenchmarksReport.create(jsonEntity))
        is JsonArray -> jsonEntity.map { BenchmarksReport.create(it) }
        else -> error("Wrong format of report. Expected object or array of objects.")
    }
}

fun mergeCompilerFlags(reports: List<BenchmarksReport>): List<String> {
    val flagsMap = mutableMapOf<String, MutableList<String>>()
    reports.forEach {
        val benchmarks = it.benchmarks.values.flatten().asSequence().filter { it.metric == BenchmarkResult.Metric.COMPILE_TIME }
                .map { it.shortName }.toList()
        if (benchmarks.isNotEmpty())
            (flagsMap.getOrPut("${it.compiler.backend.flags.joinToString()}") { mutableListOf<String>() }).addAll(benchmarks)
    }
    return flagsMap.map { (flags, benchmarks) -> "$flags for [${benchmarks.distinct().sorted().joinToString()}]" }
}

fun mergeReportsWithDetailedFlags(reports: List<BenchmarksReport>) =
        if (reports.size > 1) {
            // Merge reports.
            val detailedFlags = mergeCompilerFlags(reports)
            reports.map {
                BenchmarksReport(it.env, it.benchmarks.values.flatten(),
                        Compiler(Compiler.Backend(it.compiler.backend.type, it.compiler.backend.version, detailedFlags),
                                it.compiler.kotlinVersion))
            }.reduce { result, it -> result + it }
        } else {
            reports.first()
        }

fun main(args: Array<String>) {
    // Parse args.
    val argParser = ArgParser("benchmarksAnalyzer")

    val mainReport by argParser.argument(ArgType.String, description = "Main report for analysis")
    val compareToReport by argParser.argument(ArgType.String, description = "Report to compare to").optional()

    val output by argParser.option(ArgType.String, shortName = "o", description = "Output file")
    val epsValue by argParser.option(ArgType.Double, "eps", "e",
            "Meaningful performance changes").default(1.0)
    val useShortForm by argParser.option(ArgType.Boolean, "short", "s",
            "Show short version of report").default(false)

    argParser.parse(args)

    // Read contents of file.
    val mainBenchsReport = mergeReportsWithDetailedFlags(getBenchmarkReport(mainReport))

    val compareToBenchsReport = compareToReport?.let {
        mergeReportsWithDetailedFlags(getBenchmarkReport(it))
    }

    // Generate comparasion report.
    val summaryReport = SummaryBenchmarksReport(mainBenchsReport,
            compareToBenchsReport, epsValue)

    TextRender().print(summaryReport, useShortForm, output)
}
