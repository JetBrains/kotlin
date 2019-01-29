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

import org.jetbrains.analyzer.sendGetRequest
import org.jetbrains.analyzer.readFile
import org.jetbrains.analyzer.SummaryBenchmarksReport
import org.jetbrains.kliopt.*
import org.jetbrains.renders.*
import org.jetbrains.report.BenchmarksReport
import org.jetbrains.report.json.JsonTreeParser

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
            error("To get file from bintray, please, specify, build number from TeamCity and target" +
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

fun main(args: Array<String>) {

    val options = listOf(
            OptionDescriptor(ArgType.String(), "output", "o", "Output file"),
            OptionDescriptor(ArgType.Double(), "eps", "e", "Meaningful performance changes", "0.5"),
            OptionDescriptor(ArgType.Boolean(), "short", "s", "Show short version of report", "false"),
            OptionDescriptor(ArgType.Choice(listOf("text", "html", "teamcity")),
                    "renders", "r", "Renders for showing information", "text", isMultiple = true),
            OptionDescriptor(ArgType.String(), "user", "u", "User access information for authorization")
    )

    val arguments = listOf(
            ArgDescriptor(ArgType.String(), "mainReport", "Main report for analysis"),
            ArgDescriptor(ArgType.String(), "compareToReport", "Report to compare to", isRequired = false)
    )

    // Parse args.
    val argParser = ArgParser(options, arguments)
    if (argParser.parse(args)) {
        // Read contents of file.
        val mainBenchsResults = getFileContent(argParser.get<String>("mainReport")!!, argParser.get<String>("user"))
        val mainReportElement = JsonTreeParser.parse(mainBenchsResults)
        val mainBenchsReport = BenchmarksReport.create(mainReportElement)
        var compareToBenchsReport = argParser.get<String>("compareToReport")?.let {
            val compareToResults = getFileContent(it, argParser.get<String>("user"))
            val compareToReportElement = JsonTreeParser.parse(compareToResults)
            BenchmarksReport.create(compareToReportElement)
        }

        val renders = argParser.getAll<String>("renders")

        // Generate comparasion report.
        val summaryReport = SummaryBenchmarksReport(mainBenchsReport,
                compareToBenchsReport,
                argParser.get<Double>("eps")!!)

        var output = argParser.get<String>("output")

        renders?.forEach {
            Render.getRenderByName(it).print(summaryReport, argParser.get<Boolean>("short")!!, output)
            output = null
        }
    }
}