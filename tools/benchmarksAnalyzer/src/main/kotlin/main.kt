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

import org.jetbrains.analyzer.getEnv
import org.jetbrains.analyzer.readFile
import org.jetbrains.analyzer.SummaryBenchmarksReport
import org.jetbrains.kliopt.*
import org.jetbrains.renders.TextRender
import org.jetbrains.renders.TeamCityStatisticsRender
import org.jetbrains.report.BenchmarksReport
import org.jetbrains.report.json.JsonTreeParser

fun main(args: Array<String>) {

    val options = listOf(
            OptionDescriptor(ArgType.STRING, "output", "o", "Output file"),
            OptionDescriptor(ArgType.DOUBLE, "eps", "e", "Meaningful performance changes", "0.5"),
            OptionDescriptor(ArgType.BOOLEAN, "short", "s", "Show short version of report", "false")
    )

    val arguments = listOf(
            ArgDescriptor(ArgType.STRING, "mainReport", "Main report for analysis"),
            ArgDescriptor(ArgType.STRING, "compareToReport", "Report to compare to", isRequired = false)
    )

    // Parse args.
    val argParser = ArgParser(options, arguments)
    if (argParser.parse(args)) {
        // Read contents of file.
        val mainBenchsResults = readFile(argParser.get("mainReport")!!.stringValue)
        val mainReportElement = JsonTreeParser.parse(mainBenchsResults)
        val mainBenchsReport = BenchmarksReport.create(mainReportElement)
        var compareToBenchsReport = argParser.get("compareToReport")?.stringValue?.let {
            val compareToResults = readFile(it)
            val compareToReportElement = JsonTreeParser.parse(compareToResults)
            BenchmarksReport.create(compareToReportElement)
        }

        // Generate comparasion report
        val summaryReport = SummaryBenchmarksReport(mainBenchsReport,
                compareToBenchsReport,
                argParser.get("eps")!!.doubleValue)
        TextRender().print(summaryReport, argParser.get("short")!!.booleanValue,
                argParser.get("output")?.stringValue)
        // Produce information for TeamCity if needed.
        getEnv("TEAMCITY_BUILD_PROPERTIES_FILE")?.let {
            TeamCityStatisticsRender().print(summaryReport, argParser.get("short")!!.booleanValue)
        }
    }
}