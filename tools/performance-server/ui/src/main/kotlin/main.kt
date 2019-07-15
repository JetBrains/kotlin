/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

import kotlin.browser.*
import org.w3c.xhr.*
import org.jetbrains.report.json.*
import org.jetbrains.build.Build
import kotlin.js.*
import kotlin.math.ceil
import org.w3c.dom.*

// API for interop with JS library Chartist.
external class ChartistPlugins {
    fun legend(data: dynamic): dynamic
    fun ctAxisTitle(data: dynamic): dynamic
}

external object Chartist {
    class Svg(form: String, parameters: dynamic, chartArea: String)
    val plugins: ChartistPlugins
    val Interpolation: dynamic
    fun Line(query: String, data: dynamic, options: dynamic): dynamic
}

fun sendGetRequest(url: String) : String {
    val request = XMLHttpRequest()

    request.open("GET", url, false)
    request.send()
    if (request.status == 200.toShort()) {
        return request.responseText
    }
    error("Request to $url has status ${request.status}")
}

// Parse description with values for metrics.
fun <T : Any> separateValues(values: String, valuesContainer: MutableMap<String, MutableList<T?>>, convert: (String) -> T = { it as T }) {
    val existingSamples = mutableListOf<String>()
    val splittedValues = values.split(";")
    val insertedList = valuesContainer.values.firstOrNull()?.let { MutableList<T?>(it.size) { null } } ?: mutableListOf<T?>()
    splittedValues.forEach { it ->
        val valueParts = it.split("-", limit = 2)
        if (valueParts.size != 2) {
            error("Wrong format of value $it.")
        }
        val (sampleName, value) = valueParts
        existingSamples.add(sampleName)
        val currentList = mutableListOf<T?>()
        currentList.addAll(insertedList)
        valuesContainer.getOrPut(sampleName) { currentList }.add(convert(value))
    }
    // Check if there are other keys that are absent in current record.
    val missedSamples = valuesContainer.keys - existingSamples
    missedSamples.forEach {
        valuesContainer[it]!!.add(null)
    }
}

fun getBuildsGroup(builds: List<Build>) = buildsNumberToShow?.let {
        val buildsGroups = builds.chunked(buildsNumberToShow!!)
        val expectedGroup = buildsGroups.size - 1 + stageToShow
        val index = when {
            expectedGroup < 0 -> 0
            expectedGroup >= buildsGroups.size -> buildsGroups.size - 1
            else -> expectedGroup
        }
        buildsGroups[index]
    } ?: builds


fun getChartData(labels: List<String>, valuesList: Collection<List<*>>, stageToShow: Int = 0,
                 buildsNumber: Int? = null, classNames: Array<String>? = null): dynamic {
    val chartData: dynamic = object{}
    // Show only some part of data.
    val (labelsData, valuesData) = buildsNumber?.let {
        println("Divide on ${labels.size / buildsNumber}")
        val labelsGroups = labels.chunked(buildsNumber)
        val valuesListGroups = valuesList.map { it.chunked(buildsNumber) }
        val expectedGroup = labelsGroups.size - 1 + stageToShow
        val index = when {
            expectedGroup < 0 -> 0
            expectedGroup >= labelsGroups.size -> labelsGroups.size - 1
            else -> expectedGroup
        }
        Pair(labelsGroups[index], valuesListGroups.map {it[index]})
    } ?: Pair(labels, valuesList)
    chartData["labels"] = labelsData.toTypedArray()
    chartData["series"] = valuesData.mapIndexed { index, it ->
        val series: dynamic = object{}
        series["data"] = it.toTypedArray()
        classNames?.let { series["className"] = classNames[index] }
        series
    }.toTypedArray()
    return chartData
}

fun getChartOptions(samples: Array<String>, yTitle: String, classNames: Array<String>? = null): dynamic {
    val chartOptions: dynamic = object{}
    chartOptions["fullWidth"] = true
    val paddingObject: dynamic = object{}
    paddingObject["right"] = 40
    chartOptions["chartPadding"] = paddingObject
    val axisXObject: dynamic = object{}
    axisXObject["offset"] = 40
    axisXObject["labelInterpolationFnc"] = { value, index, labels ->
        val labelsCount = 30
        val skipNumber = ceil((labels.length as Int).toDouble() / labelsCount).toInt()
        if (skipNumber > 1) {
            if (index % skipNumber  == 0) value else null
        } else {
            value
        }
    }
    chartOptions["axisX"] = axisXObject
    val axisYObject: dynamic = object{}
    axisYObject["offset"] = 90
    chartOptions["axisY"] = axisYObject
    val legendObject: dynamic = object{}
    legendObject["legendNames"] = samples
    classNames?.let { legendObject["classNames"] = classNames }
    val titleObject: dynamic = object{}
    val axisYTitle: dynamic = object{}
    axisYTitle["axisTitle"] = yTitle
    axisYTitle["axisClass"] = "ct-axis-title"
    val titleOffset: dynamic = {}
    titleOffset["x"] = 15
    titleOffset["y"] = 15
    axisYTitle["offset"] = titleOffset
    axisYTitle["textAnchor"] = "middle"
    axisYTitle["flipTitle"] = true
    titleObject["axisY"] = axisYTitle
    val interpolationObject: dynamic = {}
    interpolationObject["fillHoles"] = true
    chartOptions["lineSmooth"] = Chartist.Interpolation.simple(interpolationObject)
    chartOptions["plugins"] = arrayOf(Chartist.plugins.legend(legendObject), Chartist.plugins.ctAxisTitle(titleObject))
    return chartOptions
}

fun redirect(url: String) {
    window.location.href = url
}

fun customizeChart(chart: dynamic, chartContainer: String, jquerySelector: dynamic, builds: List<Build>,
                   parameters: Map<String, String>) {
    chart.on("draw", { data ->
        var element = data.element
        if (data.type == "point") {
            val buildsGroup = getBuildsGroup(builds)
            val pointSize = 12
            val currentBuild = buildsGroup.get(data.index)
            // Higlight builds with failures.
            if (currentBuild.failuresNumber > 0) {
                val svgParameters: dynamic = object{}
                svgParameters["d"] = arrayOf("M", data.x, data.y - pointSize,
                        "L", data.x - pointSize, data.y + pointSize/2,
                        "L", data.x + pointSize, data.y + pointSize/2, "z").joinToString(" ")
                svgParameters["style"] = "fill:rgb(255,0,0);stroke-width:0"
                val triangle = Chartist.Svg("path", svgParameters, chartContainer)
                element = data.element.replace(triangle)
            } else if (currentBuild.buildNumber == parameters["build"]) {
                // Higlight choosen build.
                val svgParameters: dynamic = object{}
                svgParameters["x"] = data.x - pointSize/2
                svgParameters["y"] = data.y - pointSize/2
                svgParameters["height"] = pointSize
                svgParameters["width"] = pointSize
                svgParameters["style"] = "fill:rgb(0,0,255);stroke-width:0"
                val rectangle = Chartist.Svg("rect", svgParameters, "ct-point")
                element = data.element.replace(rectangle)
            }
            // Add tooltips.
            val linkToDetailedInfo = "https://kotlin-native-performance.labs.jb.gg/?report=bintray:" +
                    "${currentBuild.buildNumber}:${parameters["target"]}:nativeReport.json" +
                    "${if (data.index - 1 >= 0)
                        "&compareTo=bintray:${buildsGroup.get(data.index - 1).buildNumber}:${parameters["target"]}:nativeReport.json"
                    else ""}"
            val information = buildString {
                append("<a href=\"$linkToDetailedInfo\">${currentBuild.buildNumber}</a><br>")
                append("Value: ${data.value.y.toFixed(4)}<br>")
                if (currentBuild.failuresNumber > 0) {
                    append("failures: ${currentBuild.failuresNumber}<br>")
                }
                append("branch: ${currentBuild.branch}<br>")
                append("date: ${currentBuild.date}<br>")
                append("time: ${currentBuild.formattedStartTime}-${currentBuild.formattedFinishTime}<br>")
                append("Commits:<br>")
                val commitsList = currentBuild.commits.split(";")
                commitsList.forEach {
                    if (!it.isEmpty())
                        append("${it.substringBefore("by").substring(0, 7)} by ${it.substringAfter("by ")}<br>")
                }

            }
            element._node.setAttribute("title", information)
            element._node.setAttribute("data-chart-tooltip", chartContainer)
            element._node.addEventListener("click", {
                redirect(linkToDetailedInfo)
            })
        }
    })
    chart.on("created", {
        val currentChart = jquerySelector
        val parameters: dynamic = object{}
        parameters["selector"] = "[data-chart-tooltip=\"$chartContainer\"]"
        parameters["container"] = "#$chartContainer"
        parameters["html"] = true
        currentChart.tooltip(parameters)
    })
}

var stageToShow = 0

var buildsNumberToShow: Int? = null

fun main(args: Array<String>) {
    val serverUrl = "https://kotlin-native-perf-summary.labs.jb.gg"
    buildsNumberToShow = null
    stageToShow = 0
    val zoomRatio = 3

    // Get parameters from request.
    val url = window.location.href
    val parametersPart = url.substringAfter("?").split('&')
    val parameters = mutableMapOf("target" to "Linux", "type" to "dev", "build" to "", "branch" to "master")
    parametersPart.forEach {
        val parsedParameter = it.split("=", limit = 2)
        if (parsedParameter.size == 2) {
            val (key, value) = parsedParameter
            parameters[key] = value
        }
    }

    // Get builds.
    val buildsUrl = buildString {
        append("$serverUrl/builds")
        append("/${parameters["target"]}")
        append("/${parameters["type"]}")
        append("/${parameters["branch"]}")
        append("/${parameters["build"]}")
    }
    val response = sendGetRequest(buildsUrl)

    val data = JsonTreeParser.parse(response)
    if (data !is JsonArray) {
        error("Response is expected to be an array.")
    }
    val builds = data.jsonArray.map { Build.create(it as JsonObject) }
            .sortedWith(compareBy ( { it.buildNumber.substringBefore(".").toInt() }, { it.buildNumber.substringAfter(".").substringBefore("-").toDouble() }, { it.buildNumber.substringAfterLast("-").toInt() }))

    val branchesUrl = "$serverUrl/branches/${parameters["target"]}"

    val branches: Array<String> = JSON.parse(sendGetRequest(branchesUrl))
    val releaseBranches = branches.filter { "^v\\d+\\.\\d+\\.\\d+-fixes$".toRegex().find(it) != null }

    // Fill autocomplete list.
    val buildsNumbersUrl = "$serverUrl/buildsNumbers/${parameters["target"]}"
    val buildsNumbers: Array<String> = JSON.parse(sendGetRequest(buildsNumbersUrl))

    // Add release branches to selector.
    releaseBranches.forEach {
        val option = Option(it, it)
        js("$('#inputGroupBranch')").append(js("$(option)"))
    }

    // Change inputs values connected with parameters and add events listeners.
    document.querySelector("#inputGroupTarget [value=\"${parameters["target"]}\"]")?.setAttribute("selected", "true")
    document.querySelector("#inputGroupBuildType [value=\"${parameters["type"]}\"]")?.setAttribute("selected", "true")
    document.querySelector("#inputGroupBranch [value=\"${parameters["branch"]}\"]")?.setAttribute("selected", "true")
    (document.getElementById("highligted_build") as HTMLInputElement).value = parameters["build"]!!

    // Add onChange events for fields.
    js("$('#inputGroupTarget')").change({
        val newValue = js("$(this).val()")
        if (newValue != parameters["target"]) {
            val newLink = "http://${window.location.host}/?target=$newValue&type=${parameters["type"]}&branch=${parameters["branch"]}" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}"
            window.location.href = newLink
        }
    })
    js("$('#inputGroupBuildType')").change({
        val newValue = js("$(this).val()")
        if (newValue != parameters["type"]) {
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=$newValue&branch=${parameters["branch"]}" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}"
            window.location.href = newLink
        }
    })
    js("$('#inputGroupBranch')").change({
        val newValue = js("$(this).val()")
        if (newValue != parameters["branch"]) {
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}&branch=$newValue" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}"
            window.location.href = newLink
        }
    })

    val autocompleteParameters: dynamic = object{}
    autocompleteParameters["lookup"] = buildsNumbers
    autocompleteParameters["onSelect"] = { suggestion ->
        if (suggestion.value != parameters["build"]) {
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}" +
                    "${if ((suggestion.value as String).isEmpty()) "" else "&build=${suggestion.value}"}"
            window.location.href = newLink
        }
    }
    js("$( \"#highligted_build\" )").autocomplete(autocompleteParameters)
    js("$('#highligted_build')").change({ value ->
        val newValue = js("$(this).val()").toString()
        if (newValue.isEmpty() || newValue in builds.map {it.buildNumber}) {
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}" +
                    "${if (newValue.isEmpty()) "" else "&build=$newValue"}"
            window.location.href = newLink
        }
    })


    // Collect information for charts library.
    val labels = mutableListOf<String>()
    val executionTime = mutableMapOf<String, MutableList<Double?>>()
    val compileTime = mutableMapOf<String, MutableList<Double?>>()
    val codeSize = mutableMapOf<String, MutableList<Double?>>()
    val bundleSize = mutableListOf<Int?>()

    builds.forEach {
        // Choose labels on x axis.
        if (parameters["type"] == "day") {
            labels.add(it.date)
        } else {
            labels.add(it.buildNumber)
        }
        separateValues(it.executionTime, executionTime) { value -> value.toDouble() }
        separateValues(it.compileTime, compileTime) { value -> value.toDouble() / 1000 }
        separateValues(it.codeSize, codeSize) { value -> value.toDouble() }
        bundleSize.add(it.bundleSize?.toInt()?. let { it / 1024 / 1024 })
    }

    val sizeClassNames = arrayOf("ct-series-d", "ct-series-e")

    // Draw charts.
    val execChart = Chartist.Line("#exec_chart", getChartData(labels, executionTime.values, stageToShow, buildsNumberToShow),
            getChartOptions(executionTime.keys.toTypedArray(), "Normalized time"))
    val compileChart = Chartist.Line("#compile_chart", getChartData(labels, compileTime.values, stageToShow, buildsNumberToShow),
            getChartOptions(compileTime.keys.toTypedArray(), "Time, milliseconds"))
    val codeSizeChart = Chartist.Line("#codesize_chart", getChartData(labels, codeSize.values, stageToShow, buildsNumberToShow, sizeClassNames),
            getChartOptions(codeSize.keys.toTypedArray(), "Normalized size", arrayOf("ct-series-3", "ct-series-4")))
    val bundleSizeChart = Chartist.Line("#bundlesize_chart", getChartData(labels, listOf(bundleSize), stageToShow, buildsNumberToShow, sizeClassNames),
            getChartOptions(arrayOf("Bundle size"), "Size, MB", arrayOf("ct-series-3")))

    // Tooltips and higlights.
    customizeChart(execChart, "exec_chart", js("$(\"#exec_chart\")"), builds, parameters)
    customizeChart(compileChart, "compile_chart", js("$(\"#compile_chart\")"), builds, parameters)
    customizeChart(codeSizeChart, "codesize_chart", js("$(\"#codesize_chart\")"), builds, parameters)
    customizeChart(bundleSizeChart, "bundlesize_chart", js("$(\"#bundlesize_chart\")"), builds, parameters)

    val updateAllCharts: () -> Unit = {
        execChart.update(getChartData(labels, executionTime.values, stageToShow, buildsNumberToShow))
        compileChart.update(getChartData(labels, compileTime.values, stageToShow, buildsNumberToShow))
        codeSizeChart.update(getChartData(labels, codeSize.values, stageToShow, buildsNumberToShow, sizeClassNames))
        bundleSizeChart.update(getChartData(labels, listOf(bundleSize), stageToShow, buildsNumberToShow, sizeClassNames))
    }

    js("$('#plusBtn')").click({
        buildsNumberToShow = buildsNumberToShow?.let {
            if (it / zoomRatio > zoomRatio) {
                it / zoomRatio
            } else {
                it
            }
        } ?: labels.size / zoomRatio
        println(buildsNumberToShow)
        updateAllCharts()
    })

    js("$('#minusBtn')").click({
        buildsNumberToShow = buildsNumberToShow?.let {
            if (it * zoomRatio <= labels.size) {
                it * zoomRatio
            } else {
                null
            }
        }
        updateAllCharts()
    })

    js("$('#prevBtn')").click({
        buildsNumberToShow?.let {
            val bottomBorder = -labels.size / (buildsNumberToShow as Int)
            if (stageToShow - 1 > bottomBorder) {
                stageToShow--
            }
        } ?: run { stageToShow = 0}
        updateAllCharts()
    })

    js("$('#nextBtn')").click({
        if (stageToShow + 1 <= 0) {
            stageToShow++
        }
        updateAllCharts()
    })

    // Auto reload.
    parameters["refresh"]?.let {
        // Set event.
        window.setInterval({
            window.location.reload()
        }, it.toInt() * 1000)
    }
}