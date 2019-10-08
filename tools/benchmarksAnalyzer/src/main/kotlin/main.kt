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
@file:UseExperimental(ExperimentalCli::class)
import org.jetbrains.analyzer.sendGetRequest
import org.jetbrains.analyzer.readFile
import org.jetbrains.analyzer.SummaryBenchmarksReport
import kotlinx.cli.*
import org.jetbrains.renders.*
import org.jetbrains.report.*
import org.jetbrains.report.json.*

abstract class Connector {
    abstract val connectorPrefix: String

    fun isCompatible(fileName: String) =
            fileName.startsWith(connectorPrefix)

    abstract fun getFileContent(fileLocation: String, user: String? = null): String
}

object BintrayConnector : Connector() {
    override val connectorPrefix = "bintray:"
    val bintrayUrl = "https://dl.bintray.com/content/lepilkinaelena/KotlinNativePerformance"

    override fun getFileContent(fileLocation: String, user: String?): String {
        val fileParametersSize = 3
        val fileDescription = fileLocation.substringAfter(connectorPrefix)
        val fileParameters = fileDescription.split(':', limit = fileParametersSize)

        // Right link to bintray file.
        if (fileParameters.size == 1) {
            val accessFileUrl = "$bintrayUrl/${fileParameters[0]}"
            return sendGetRequest(accessFileUrl, followLocation = true)
        }
        // Used builds description format.
        if (fileParameters.size != fileParametersSize) {
            error("To get file from bintray, please, specify, build number from TeamCity and target" +
                    " in format bintray:build_number:target:filename")
        }
        val (buildNumber, target, fileName) = fileParameters
        val accessFileUrl = "$bintrayUrl/$target/$buildNumber/$fileName"
        return sendGetRequest(accessFileUrl, followLocation = true)
    }
}

object TeamCityConnector: Connector() {
    override val connectorPrefix = "teamcity:"
    val teamCityUrl = "http://buildserver.labs.intellij.net"

    override fun getFileContent(fileLocation: String, user: String?): String {
        val fileDescription = fileLocation.substringAfter(connectorPrefix)
        val buildLocator = fileDescription.substringBeforeLast(':')
        val fileName = fileDescription.substringAfterLast(':')
        if (fileDescription == fileLocation ||
                fileDescription == buildLocator || fileName == fileDescription) {
            error("To get file from TeamCity, please, specify, build locator and filename on TeamCity" +
                    " in format teamcity:build_locator:filename")
        }
        val accessFileUrl = "$teamCityUrl/app/rest/builds/$buildLocator/artifacts/content/$fileName"
        val userName = user?.substringBefore(':')
        val password = user?.substringAfter(':')
        return sendGetRequest(accessFileUrl, userName, password)
    }
}

fun getFileContent(fileName: String, user: String? = null): String {
    return when {
        BintrayConnector.isCompatible(fileName) -> BintrayConnector.getFileContent(fileName, user)
        TeamCityConnector.isCompatible(fileName) -> TeamCityConnector.getFileContent(fileName, user)
        else -> readFile(fileName)
    }
}

fun getBenchmarkReport(fileName: String, user: String? = null): List<BenchmarksReport> {
    val jsonEntity = JsonTreeParser.parse(getFileContent(fileName, user))
    return when (jsonEntity) {
        is JsonObject -> listOf(BenchmarksReport.create(jsonEntity))
        is JsonArray -> jsonEntity.map { BenchmarksReport.create(it) }
        else -> error("Wrong format of report. Expected object or array of objects.")
    }
}

fun parseNormalizeResults(results: String): Map<String, Map<String, Double>> {
    val parsedNormalizeResults = mutableMapOf<String, MutableMap<String, Double>>()
    val tokensNumber = 3
    results.lines().forEach {
        if (!it.isEmpty()) {
            val tokens = it.split(",").map { it.trim() }
            if (tokens.size != tokensNumber) {
                error("Data for normalization should include benchmark name, metric name and value. Got $it")
            }
            parsedNormalizeResults.getOrPut(tokens[0], { mutableMapOf<String, Double>() })[tokens[1]] = tokens[2].toDouble()
        }
    }
    return parsedNormalizeResults
}

fun mergeCompilerFlags(reports: List<BenchmarksReport>) =
    reports.map {
        val benchmarks = it.benchmarks.values.flatten().asSequence().filter { it.metric == BenchmarkResult.Metric.COMPILE_TIME }
                .map { it.shortName }.distinct().sorted().joinToString()
        "${it.compiler.backend.flags.joinToString()} for [$benchmarks]"
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
    class Summary: Subcommand("summary") {
        val exec by option(ArgType.Choice(listOf("samples", "geomean")),
                description = "Execution time way of calculation").default("geomean")
        val execSamples by option(ArgType.String, "exec-samples",
                description = "Samples used for execution time metric (value 'all' allows use all samples)")
                .delimiter(",")
        val execNormalize by option(ArgType.String, "exec-normalize",
                description = "File with golden results which should be used for normalization")
        val compile by option(ArgType.Choice(listOf("samples", "geomean")),
                description = "Compile time way of calculation").default("geomean")
        val compileSamples by option(ArgType.String, "compile-samples",
                description = "Samples used for compile time metric (value 'all' allows use all samples)")
                .delimiter(",")
        val compileNormalize by option(ArgType.String, "compile-normalize",
                description = "File with golden results which should be used for normalization")
        val codesize by option(ArgType.Choice(listOf("samples", "geomean")),
                description = "Code size way of calculation").default("geomean")
        val codesizeSamples by option(ArgType.String, "codesize-samples",
                description = "Samples used for code size metric (value 'all' allows use all samples)").delimiter(",")
        val codesizeNormalize by option(ArgType.String, "codesize-normalize",
                description = "File with golden results which should be used for normalization")
        val user by option(ArgType.String, shortName = "u", description = "User access information for authorization")
        val mainReport by argument(ArgType.String, description = "Main report for analysis")

        override fun execute() {
            val reportsList = getBenchmarkReport(mainReport, user)
            val report = reportsList.reduce { result, it ->
                result.merge(it)
            }
            val benchsReport = SummaryBenchmarksReport(report)
            val results = mutableListOf<String>()
            val executionNormalize = execNormalize?.let {
                parseNormalizeResults(getFileContent(it))
            }
            val compileNormalize = compileNormalize?.let {
                parseNormalizeResults(getFileContent(it))
            }
            val codesizeNormalize = codesizeNormalize?.let {
                parseNormalizeResults(getFileContent(it))
            }

            results.apply {
                add(benchsReport.failedBenchmarks.size.toString())
                if (!execSamples.isEmpty()) {
                    val filterExec = if (execSamples.first() == "all") null else execSamples
                    add(benchsReport.getResultsByMetric(BenchmarkResult.Metric.EXECUTION_TIME,
                            exec == "geomean", filterExec, executionNormalize).joinToString(";"))
                }

                if (!compileSamples.isEmpty()) {
                    val filterCompile = if (compileSamples.first() == "all") null else compileSamples
                    add(benchsReport.getResultsByMetric(BenchmarkResult.Metric.COMPILE_TIME,
                            compile == "geomean", filterCompile, compileNormalize).joinToString(";"))
                }

                if (!codesizeSamples.isEmpty()) {
                    val filterCodesize = if (codesizeSamples.first() == "all") null else codesizeSamples
                    add(benchsReport.getResultsByMetric(BenchmarkResult.Metric.CODE_SIZE,
                            codesize == "geomean", filterCodesize, codesizeNormalize).joinToString(";"))
                }

            }
            println(results.joinToString())
        }
    }
    val action = Summary()
    // Parse args.
    val argParser = ArgParser("benchmarksAnalyzer")
    argParser.subcommands(action)
    val mainReport by argParser.argument(ArgType.String, description = "Main report for analysis")
    val compareToReport by argParser.argument(ArgType.String, description = "Report to compare to").optional()

    val output by argParser.option(ArgType.String, shortName = "o", description = "Output file")
    val epsValue by argParser.option(ArgType.Double, "eps", "e",
            "Meaningful performance changes").default(1.0)
    val useShortForm by argParser.option(ArgType.Boolean, "short", "s",
            "Show short version of report").default(false)
    val renders by argParser.option(ArgType.Choice(listOf("text", "html", "teamcity", "statistics", "metrics")),
        shortName = "r", description = "Renders for showing information").multiple().default(listOf("text"))
    val user by argParser.option(ArgType.String, shortName = "u", description = "User access information for authorization")

    if (argParser.parse(args).commandName == "benchmarksAnalyzer") {
        // Read contents of file.
        val mainBenchsReport = mergeReportsWithDetailedFlags(getBenchmarkReport(mainReport, user))

        var compareToBenchsReport = compareToReport?.let {
            mergeReportsWithDetailedFlags(getBenchmarkReport(it, user))
        }

        // Generate comparasion report.
        val summaryReport = SummaryBenchmarksReport(mainBenchsReport,
                compareToBenchsReport,
                epsValue)

        var outputFile = output
        renders.forEach {
            Render.getRenderByName(it).print(summaryReport, useShortForm, outputFile)
            outputFile = null
        }
    }
}