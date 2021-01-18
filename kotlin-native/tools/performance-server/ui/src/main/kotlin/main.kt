/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.browser.*
import org.w3c.fetch.*
import org.jetbrains.report.json.*
import org.jetbrains.buildInfo.Build
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

data class Commit(val revision: String, val developer: String)

fun sendGetRequest(url: String) = window.fetch(url, RequestInit("GET")).then { response ->
    if (!response.ok)
        error("Error during getting response from $url\n" +
                "${response}")
    else
        response.text()
}.then { text -> text }

// Get data for chart in needed format.
fun getChartData(labels: List<String>, valuesList: Collection<List<*>>,
                 classNames: Array<String>? = null): dynamic {
    val chartData: dynamic = object {}
    chartData["labels"] = labels.toTypedArray()
    chartData["series"] = valuesList.mapIndexed { index, it ->
        val series: dynamic = object {}
        series["data"] = it.toTypedArray()
        classNames?.let { series["className"] = classNames[index] }
        series
    }.toTypedArray()
    return chartData
}

// Create object with options of chart.
fun getChartOptions(samples: Array<String>, yTitle: String, classNames: Array<String>? = null): dynamic {
    val chartOptions: dynamic = object {}
    chartOptions["fullWidth"] = true
    val paddingObject: dynamic = object {}
    paddingObject["right"] = 40
    chartOptions["chartPadding"] = paddingObject
    val axisXObject: dynamic = object {}
    axisXObject["offset"] = 40
    axisXObject["labelInterpolationFnc"] = { value, index, labels ->
        val labelsCount = 20
        val skipNumber = ceil((labels.length as Int).toDouble() / labelsCount).toInt()
        if (skipNumber > 1) {
            if (index % skipNumber == 0) value else null
        } else {
            value
        }
    }
    chartOptions["axisX"] = axisXObject
    val axisYObject: dynamic = object {}
    axisYObject["offset"] = 90
    chartOptions["axisY"] = axisYObject
    val legendObject: dynamic = object {}
    legendObject["legendNames"] = samples
    classNames?.let { legendObject["classNames"] = classNames.sliceArray(0 until samples.size) }
    val titleObject: dynamic = object {}
    val axisYTitle: dynamic = object {}
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

// Set customizations rules for chart.
fun customizeChart(chart: dynamic, chartContainer: String, jquerySelector: dynamic, builds: List<Build?>,
                   parameters: Map<String, String>) {
    chart.on("draw", { data ->
        var element = data.element
        if (data.type == "point") {
            val pointSize = 12
            val currentBuild = builds.get(data.index)
            currentBuild?.let { currentBuild ->
                // Higlight builds with failures.
                if (currentBuild.failuresNumber > 0) {
                    val svgParameters: dynamic = object {}
                    svgParameters["d"] = arrayOf("M", data.x, data.y - pointSize,
                            "L", data.x - pointSize, data.y + pointSize / 2,
                            "L", data.x + pointSize, data.y + pointSize / 2, "z").joinToString(" ")
                    svgParameters["style"] = "fill:rgb(255,0,0);stroke-width:0"
                    val triangle = Chartist.Svg("path", svgParameters, chartContainer)
                    element = data.element.replace(triangle)
                } else if (currentBuild.buildNumber == parameters["build"]) {
                    // Higlight choosen build.
                    val svgParameters: dynamic = object {}
                    svgParameters["x"] = data.x - pointSize / 2
                    svgParameters["y"] = data.y - pointSize / 2
                    svgParameters["height"] = pointSize
                    svgParameters["width"] = pointSize
                    svgParameters["style"] = "fill:rgb(0,0,255);stroke-width:0"
                    val rectangle = Chartist.Svg("rect", svgParameters, "ct-point")
                    element = data.element.replace(rectangle)
                }
                // Add tooltips.
                var shift = 1
                var previousBuild: Build? = null
                while (previousBuild == null && data.index - shift >= 0) {
                    previousBuild = builds.get(data.index - shift)
                    shift++
                }
                val linkToDetailedInfo = "https://kotlin-native-performance.labs.jb.gg/?report=" +
                        "${currentBuild.buildNumber}:${parameters["target"]}" +
                        "${previousBuild?.let {
                            "&compareTo=${previousBuild.buildNumber}:${parameters["target"]}"
                        } ?: ""}"
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
                    val commitsList = (JsonTreeParser.parse("{${currentBuild.commits}}") as JsonObject).getArray("commits").map {
                        Commit(
                                (it as JsonObject).getPrimitive("revision").content,
                                (it as JsonObject).getPrimitive("developer").content
                        )
                    }
                    val commits = if (commitsList.size > 3) commitsList.slice(0..2) else commitsList
                    commits.forEach {
                        append("${it.revision.substring(0, 7)} by ${it.developer}<br>")
                    }
                    if (commitsList.size > 3) {
                        append("...")
                    }
                }
                element._node.setAttribute("title", information)
                element._node.setAttribute("data-chart-tooltip", chartContainer)
                element._node.addEventListener("click", {
                    redirect(linkToDetailedInfo)
                })
            }
        }
    })
    chart.on("created", {
        val currentChart = jquerySelector
        val parameters: dynamic = object {}
        parameters["selector"] = "[data-chart-tooltip=\"$chartContainer\"]"
        parameters["container"] = "#$chartContainer"
        parameters["html"] = true
        currentChart.tooltip(parameters)
    })
}

var buildsNumberToShow: Int = 200
var beforeDate: String? = null
var afterDate: String? = null

external fun decodeURIComponent(url: String): String
external fun encodeURIComponent(url: String): String

fun getDatesComponents() = "${beforeDate?.let {"&before=${encodeURIComponent(it)}"} ?: ""}" +
        "${afterDate?.let {"&after=${encodeURIComponent(it)}"} ?: ""}"

fun main(args: Array<String>) {
    val serverUrl = "https://kotlin-native-perf-summary.labs.jb.gg"
    val zoomRatio = 2

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

    buildsNumberToShow = parameters["count"]?.toInt() ?: buildsNumberToShow
    beforeDate = parameters["before"]?.let { decodeURIComponent(it) }
    afterDate = parameters["after"]?.let { decodeURIComponent(it) }

    // Get branches.
    val branchesUrl = "$serverUrl/branches"
    sendGetRequest(branchesUrl).then { response ->
        val branches: Array<String> = JSON.parse(response)
        // Add release branches to selector.
        branches.filter { it != "master" }.forEach {
            if ("v(\\d|\\.)+(-M\\d)?-fixes".toRegex().matches(it)) {
                val option = Option(it, it)
                js("$('#inputGroupBranch')").append(js("$(option)"))
            }
        }
        document.querySelector("#inputGroupBranch [value=\"${parameters["branch"]}\"]")?.setAttribute("selected", "true")
    }


    // Fill autocomplete list with build numbers.
    val buildsNumbersUrl = "$serverUrl/buildsNumbers/${parameters["target"]}"
    sendGetRequest(buildsNumbersUrl).then { response ->
        val buildsNumbers: Array<String> = JSON.parse(response)
        val autocompleteParameters: dynamic = object {}
        autocompleteParameters["lookup"] = buildsNumbers
        autocompleteParameters["onSelect"] = { suggestion ->
            if (suggestion.value != parameters["build"]) {
                val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}" +
                        "${if ((suggestion.value as String).isEmpty()) "" else "&build=${suggestion.value}"}&count=$buildsNumberToShow" +
                        getDatesComponents()
                window.location.href = newLink
            }
        }
        js("$( \"#highligted_build\" )").autocomplete(autocompleteParameters)
        js("$('#highligted_build')").change({ value ->
            val newValue = js("$(this).val()").toString()
            if (newValue.isEmpty() || newValue in buildsNumbers) {
                val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}" +
                        "${if (newValue.isEmpty()) "" else "&build=$newValue"}&count=$buildsNumberToShow" +
                        getDatesComponents()
                window.location.href = newLink
            }
        })
    }

    // Change inputs values connected with parameters and add events listeners.
    document.querySelector("#inputGroupTarget [value=\"${parameters["target"]}\"]")?.setAttribute("selected", "true")
    document.querySelector("#inputGroupBuildType [value=\"${parameters["type"]}\"]")?.setAttribute("selected", "true")
    (document.getElementById("highligted_build") as HTMLInputElement).value = parameters["build"]!!

    // Add onChange events for fields.
    // Don't use AJAX to have opportunity to share results with simple links.
    js("$('#inputGroupTarget')").change({
        val newValue = js("$(this).val()")
        if (newValue != parameters["target"]) {
            val newLink = "http://${window.location.host}/?target=$newValue&type=${parameters["type"]}&branch=${parameters["branch"]}" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}&count=$buildsNumberToShow"
            window.location.href = newLink
        }
    })
    js("$('#inputGroupBuildType')").change({
        val newValue = js("$(this).val()")
        if (newValue != parameters["type"]) {
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=$newValue&branch=${parameters["branch"]}" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}&count=$buildsNumberToShow"
            window.location.href = newLink
        }
    })
    js("$('#inputGroupBranch')").change({
        val newValue = js("$(this).val()")
        if (newValue != parameters["branch"]) {
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}&branch=$newValue" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}&count=$buildsNumberToShow"
            window.location.href = newLink
        }
    })

    val platformSpecificBenchs = if (parameters["target"] == "Mac_OS_X") ",FrameworkBenchmarksAnalyzer,SpaceFramework_iosX64" else
        if (parameters["target"] == "Linux") ",kotlinx.coroutines" else ""

    var execData = listOf<String>() to listOf<List<Double?>>()
    var compileData = listOf<String>() to listOf<List<Double?>>()
    var codeSizeData = listOf<String>() to listOf<List<Double?>>()
    var bundleSizeData = listOf<String>() to listOf<List<Int?>>()

    val sizeClassNames = arrayOf("ct-series-e", "ct-series-f", "ct-series-g")

    // Draw charts.
    var execChart: dynamic = null
    var compileChart: dynamic = null
    var codeSizeChart: dynamic = null
    var bundleSizeChart: dynamic = null

    val descriptionUrl = "$serverUrl/buildsDesc/${parameters["target"]}?type=${parameters["type"]}" +
            "${if (parameters["branch"] != "all") "&branch=${parameters["branch"]}" else ""}&count=$buildsNumberToShow" +
            getDatesComponents()

    val metricUrl = "$serverUrl/metricValue/${parameters["target"]}/"

    val unstableBenchmarksPromise = sendGetRequest("$serverUrl/unstable").then { response ->
        val unstableList = response as String
        val data = JsonTreeParser.parse(unstableList)
        if (data !is JsonArray) {
            error("Response is expected to be an array.")
        }
        data.jsonArray.map {
            (it as JsonPrimitive).content
        }
    }

    // Get builds description.
    val buildsInfoPromise = sendGetRequest(descriptionUrl).then { response ->
        val buildsInfo = response as String
        val data = JsonTreeParser.parse(buildsInfo)
        if (data !is JsonArray) {
            error("Response is expected to be an array.")
        }
        data.jsonArray.map {
            val element = it as JsonElement
            if (element.isNull) null else Build.create(element as JsonObject)
        }
    }

    unstableBenchmarksPromise.then { unstableBenchmarks ->
        // Collect information for charts library.
        val valuesToShow = mapOf("EXECUTION_TIME" to listOf(mapOf(
                        "normalize" to "true"
                ),
                mapOf(
                        "normalize" to "true",
                        "exclude" to unstableBenchmarks.joinToString(",")
                )),
                "COMPILE_TIME" to listOf(mapOf(
                        "samples" to "HelloWorld,Videoplayer$platformSpecificBenchs",
                        "agr" to "samples"
                )),
                "CODE_SIZE" to listOf(mapOf(
                        "normalize" to "true",
                        "exclude" to if (parameters["target"] == "Linux")
                            "kotlinx.coroutines"
                        else if (parameters["target"] == "Mac_OS_X")
                            "SpaceFramework_iosX64"
                        else ""
                ), if (platformSpecificBenchs.isNotEmpty()) mapOf(
                        "normalize" to "true",
                        "agr" to "samples",
                        "samples" to platformSpecificBenchs.removePrefix(",")
                ) else null).filterNotNull(),
                "BUNDLE_SIZE" to listOf(mapOf("samples" to "KotlinNative",
                        "agr" to "samples"))
        )
        // Send requests to get all needed metric values.
        valuesToShow.map { (metric, listOfSettings) ->
            val resultValues = listOfSettings.map { settings ->
                val getParameters = with(StringBuilder()) {
                    if (settings.isNotEmpty()) {
                        append("?")
                    }
                    var prefix = ""
                    settings.forEach { (key, value) ->
                        if (value.isNotEmpty()) {
                            append("$prefix$key=$value")
                            prefix = "&"
                        }
                    }
                    toString()
                }
                val branchParameter = if (parameters["branch"] != "all")
                    (if (getParameters.isEmpty()) "?" else "&") + "branch=${parameters["branch"]}"
                else ""

                val url = "$metricUrl$metric$getParameters$branchParameter${
                    if (parameters["type"] != "all")
                        (if (getParameters.isEmpty() && branchParameter.isEmpty()) "?" else "&") + "type=${parameters["type"]}"
                    else ""
                }&count=$buildsNumberToShow${getDatesComponents()}"
                sendGetRequest(url)
            }.toTypedArray()

            // Get metrics values for charts.
            Promise.all(resultValues).then { responses ->
                val valuesList = responses.map { response ->
                    val results = (JsonTreeParser.parse(response) as JsonArray).map {
                        (it as JsonObject).getPrimitive("first").content to
                                it.getArray("second").map { (it as JsonPrimitive).doubleOrNull }
                    }

                    val labels = results.map { it.first }
                    val values = results[0]?.second?.size?.let { (0..it - 1).map { i -> results.map { it.second[i] } } }
                            ?: emptyList()
                    labels to values
                }
                val labels = valuesList[0].first

                val values = valuesList.map { it.second }.reduce { acc, valuesPart -> acc + valuesPart }

                when (metric) {
                    // Update chart with gotten data.
                    "COMPILE_TIME" -> {
                        compileData = labels to values.map { it.map { it?.let { it / 1000 } } }
                        compileChart = Chartist.Line("#compile_chart",
                                getChartData(labels, compileData.second),
                                getChartOptions(valuesToShow["COMPILE_TIME"]!![0]!!["samples"]!!.split(',').toTypedArray(),
                                        "Time, milliseconds"))
                        buildsInfoPromise.then { builds ->
                            customizeChart(compileChart, "compile_chart", js("$(\"#compile_chart\")"), builds, parameters)
                            compileChart.update(getChartData(compileData.first, compileData.second))
                        }
                    }
                    "EXECUTION_TIME" -> {
                        execData = labels to values
                        execChart = Chartist.Line("#exec_chart",
                                getChartData(labels, execData.second),
                                getChartOptions(arrayOf("Geometric Mean (All)", "Geometric mean (Stable)"),
                                        "Normalized time"))
                        buildsInfoPromise.then { builds ->
                            customizeChart(execChart, "exec_chart", js("$(\"#exec_chart\")"), builds, parameters)
                            execChart.update(getChartData(execData.first, execData.second))
                        }
                    }
                    "CODE_SIZE" -> {
                        codeSizeData = labels to values
                        codeSizeChart = Chartist.Line("#codesize_chart",
                                getChartData(labels, codeSizeData.second),
                                getChartOptions(arrayOf("Geometric Mean") + platformSpecificBenchs.split(',')
                                        .filter { it.isNotEmpty() },
                                        "Normalized size",
                                        arrayOf("ct-series-4", "ct-series-5", "ct-series-6")))
                        buildsInfoPromise.then { builds ->
                            customizeChart(codeSizeChart, "codesize_chart", js("$(\"#codesize_chart\")"), builds, parameters)
                            codeSizeChart.update(getChartData(codeSizeData.first, codeSizeData.second, sizeClassNames))
                        }
                    }
                    "BUNDLE_SIZE" -> {
                        bundleSizeData = labels to values.map { it.map { it?.let { it.toInt() / 1024 / 1024 } } }
                        bundleSizeChart = Chartist.Line("#bundlesize_chart",
                                getChartData(labels,
                                        bundleSizeData.second, sizeClassNames),
                                getChartOptions(arrayOf("Bundle size"), "Size, MB", arrayOf("ct-series-4")))
                        buildsInfoPromise.then { builds ->
                            customizeChart(bundleSizeChart, "bundlesize_chart", js("$(\"#bundlesize_chart\")"), builds, parameters)
                            bundleSizeChart.update(getChartData(bundleSizeData.first, bundleSizeData.second, sizeClassNames))
                        }
                    }
                    else -> error("No chart for metric $metric")
                }
                true
            }
        }
    }

    // Update all charts with using same data.
    val updateAllCharts: () -> Unit = {
        execChart.update(getChartData(execData.first, execData.second))
        compileChart.update(getChartData(compileData.first, compileData.second))
        codeSizeChart.update(getChartData(codeSizeData.first, codeSizeData.second, sizeClassNames))
        bundleSizeChart.update(getChartData(bundleSizeData.first, bundleSizeData.second, sizeClassNames))
    }

    js("$('#plusBtn')").click({
        buildsNumberToShow =
            if (buildsNumberToShow / zoomRatio > zoomRatio) {
                buildsNumberToShow / zoomRatio
            } else {
                buildsNumberToShow
            }

        val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}&branch=${parameters["branch"]}" +
                "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}&count=$buildsNumberToShow" +
                getDatesComponents()
        window.location.href = newLink
        Unit
    })

    js("$('#minusBtn')").click({
        buildsNumberToShow = buildsNumberToShow * zoomRatio
        val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}&branch=${parameters["branch"]}" +
                "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}&count=$buildsNumberToShow" +
                getDatesComponents()
        window.location.href = newLink
        Unit
    })

    js("$('#prevBtn')").click({
        buildsInfoPromise.then { builds ->
            beforeDate = builds.firstOrNull()?.startTime
            afterDate = null
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}&branch=${parameters["branch"]}" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}&count=$buildsNumberToShow" +
                    "${beforeDate?.let {"&before=${encodeURIComponent(it)}"} ?: ""}"
            window.location.href = newLink
        }
    })

    js("$('#nextBtn')").click({
        buildsInfoPromise.then { builds ->
            beforeDate = null
            afterDate = builds.lastOrNull()?.startTime
            val newLink = "http://${window.location.host}/?target=${parameters["target"]}&type=${parameters["type"]}&branch=${parameters["branch"]}" +
                    "${if (parameters["build"]!!.isEmpty()) "" else "&build=${parameters["build"]}"}&count=$buildsNumberToShow" +
                    "${afterDate?.let {"&after=${encodeURIComponent(it)}"} ?: ""}"
            window.location.href = newLink
        }
    })

    // Auto reload.
    parameters["refresh"]?.let {
        // Set event.
        window.setInterval({
            window.location.reload()
        }, it.toInt() * 1000)
    }
}