/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlinx.cli.*
import org.jetbrains.analyzer.sendGetRequest
import org.jetbrains.analyzer.readFile
import org.jetbrains.analyzer.SummaryBenchmarksReport
import org.jetbrains.renders.*
import org.jetbrains.report.*
import org.jetbrains.report.json.*

abstract class Connector {
    abstract val connectorPrefix: String

    fun isCompatible(fileName: String) =
            fileName.startsWith(connectorPrefix)

    abstract fun getFileContent(fileLocation: String, user: String? = null): String
}

object ArtifactoryConnector : Connector() {
    override val connectorPrefix = "artifactory:"
    val artifactoryUrl = "https://repo.labs.intellij.net/kotlin-native-benchmarks"

    override fun getFileContent(fileLocation: String, user: String?): String {
        val fileParametersSize = 3
        val fileDescription = fileLocation.substringAfter(connectorPrefix)
        val fileParameters = fileDescription.split(':', limit = fileParametersSize)

        // Right link to Artifactory file.
        if (fileParameters.size == 1) {
            val accessFileUrl = "$artifactoryUrl/${fileParameters[0]}"
            return sendGetRequest(accessFileUrl, followLocation = true)
        }
        // Used builds description format.
        if (fileParameters.size != fileParametersSize) {
            error("To get file from Artifactory, please, specify, build number from TeamCity and target" +
                    " in format artifactory:build_number:target:filename")
        }
        val (buildNumber, target, fileName) = fileParameters
        val accessFileUrl = "$artifactoryUrl/$target/$buildNumber/$fileName"
        return sendGetRequest(accessFileUrl, followLocation = true)
    }
}

object TeamCityConnector : Connector() {
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

object DBServerConnector : Connector() {
    override val connectorPrefix = ""
    val serverUrl = "https://kotlin-native-perf-summary.labs.jb.gg"

    override fun getFileContent(fileLocation: String, user: String?): String {
        val buildNumber = fileLocation.substringBefore(':')
        val target = fileLocation.substringAfter(':')
        if (target == buildNumber) {
            error("To get file from database, please, specify, target and build number" +
                    " in format target:build_number")
        }
        val accessFileUrl = "$serverUrl/report/$target/$buildNumber"
        return sendGetRequest(accessFileUrl)
    }

    fun getUnstableBenchmarks(): List<String>? {
        try {
            val unstableList = sendGetRequest("$serverUrl/unstable")
            val data = JsonTreeParser.parse(unstableList)
            if (data !is JsonArray) {
                return null
            }
            return data.jsonArray.map {
                (it as JsonPrimitive).content
            }
        } catch (e: Exception) {
            return null
        }
    }
}

fun getFileContent(fileName: String, user: String? = null): String {
    return when {
        ArtifactoryConnector.isCompatible(fileName) -> ArtifactoryConnector.getFileContent(fileName, user)
        TeamCityConnector.isCompatible(fileName) -> TeamCityConnector.getFileContent(fileName, user)
        fileName.endsWith(".json") -> readFile(fileName)
        else -> DBServerConnector.getFileContent(fileName, user)
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
    val renders by argParser.option(ArgType.Choice<RenderType>(), shortName = "r",
            description = "Renders for showing information").multiple().default(listOf(RenderType.TEXT))
    val user by argParser.option(ArgType.String, shortName = "u", description = "User access information for authorization")

    argParser.parse(args)
    // Get unstable benchmarks.
    val unstableBenchmarks = DBServerConnector.getUnstableBenchmarks()

    unstableBenchmarks ?: println("Failed to get access to server and get unstable benchmarks list!")

    // Read contents of file.
    val mainBenchsReport = mergeReportsWithDetailedFlags(getBenchmarkReport(mainReport, user))

    var compareToBenchsReport = compareToReport?.let {
        mergeReportsWithDetailedFlags(getBenchmarkReport(it, user))
    }

    // Generate comparasion report.
    val summaryReport = SummaryBenchmarksReport(mainBenchsReport,
            compareToBenchsReport, epsValue,
            unstableBenchmarks ?: emptyList())

    var outputFile = output
    renders.forEach {
        it.createRender().print(summaryReport, useShortForm, outputFile)
        outputFile = null
    }
}
