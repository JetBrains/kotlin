/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.w3c.xhr.*
import kotlin.js.json
import kotlin.js.Date
import kotlin.js.Promise
import org.jetbrains.database.*
import org.jetbrains.report.json.*
import org.jetbrains.elastic.*
import org.jetbrains.network.*
import org.jetbrains.buildInfo.Build
import org.jetbrains.analyzer.*
import org.jetbrains.report.*
import org.jetbrains.utils.*

// TODO - create DSL for ES requests?

const val teamCityUrl = "https://buildserver.labs.intellij.net/app/rest"
const val artifactoryUrl = "https://repo.labs.intellij.net/kotlin-native-benchmarks"

operator fun <K, V> Map<K, V>?.get(key: K) = this?.get(key)

fun getArtifactoryHeader(artifactoryApiKey: String) = Pair("X-JFrog-Art-Api", artifactoryApiKey)

external fun decodeURIComponent(url: String): String

// Convert saved old report to expected new format.
internal fun convertToNewFormat(data: JsonObject): List<Any> {
    val env = Environment.create(data.getRequiredField("env"))
    val benchmarksObj = data.getRequiredField("benchmarks")
    val compilerDescription = data.getRequiredField("kotlin")
    val compiler = Compiler.create(compilerDescription)
    val backend = (compilerDescription as JsonObject).getRequiredField("backend")
    val flagsArray = (backend as JsonObject).getOptionalField("flags")
    var flags: List<String> = emptyList()
    if (flagsArray != null && flagsArray is JsonArray) {
        flags = flagsArray.jsonArray.map { (it as JsonLiteral).unquoted() }
    }
    val benchmarksList = parseBenchmarksArray(benchmarksObj)

    return listOf(env, compiler, benchmarksList, flags)
}

// Convert data results to expected format.
internal fun convert(json: String, buildNumber: String, target: String): List<BenchmarksReport> {
    val data = JsonTreeParser.parse(json)
    val reports = if (data is JsonArray) {
        data.map { convertToNewFormat(it as JsonObject) }
    } else listOf(convertToNewFormat(data as JsonObject))

    // Restored flags for old reports.
    val knownFlags = mapOf(
            "Cinterop" to listOf("-opt"),
            "FrameworkBenchmarksAnalyzer" to listOf("-g"),
            "HelloWorld" to if (target == "Mac OS X")
                listOf("-Xcache-directory=/Users/teamcity/buildAgent/work/c104dee5223a31c5/test_dist/klib/cache/macos_x64-gSTATIC", "-g")
            else listOf("-g"),
            "Numerical" to listOf("-opt"),
            "ObjCInterop" to listOf("-opt"),
            "Ring" to listOf("-opt"),
            "Startup" to listOf("-opt"),
            "swiftInterop" to listOf("-opt"),
            "Videoplayer" to if (target == "Mac OS X")
                listOf("-Xcache-directory=/Users/teamcity/buildAgent/work/c104dee5223a31c5/test_dist/klib/cache/macos_x64-gSTATIC", "-g")
            else listOf("-g")
    )

    return reports.map { elements ->
        val benchmarks = (elements[2] as List<BenchmarkResult>).groupBy { it.name.substringBefore('.').substringBefore(':') }
        val parsedFlags = elements[3] as List<String>
        benchmarks.map { (setName, results) ->
            val flags = if (parsedFlags.isNotEmpty() && parsedFlags[0] == "-opt") knownFlags[setName]!! else parsedFlags
            val savedCompiler = elements[1] as Compiler
            val compiler = Compiler(Compiler.Backend(savedCompiler.backend.type, savedCompiler.backend.version, flags),
                    savedCompiler.kotlinVersion)
            val newReport = BenchmarksReport(elements[0] as Environment, results, compiler)
            newReport.buildNumber = buildNumber
            newReport
        }
    }.flatten()
}

// Golden result value used to get normalized results.
data class GoldenResult(val benchmarkName: String, val metric: String, val value: Double)
data class GoldenResultsInfo(val goldenResults: Array<GoldenResult>)

// Convert information about golden results to benchmarks report format.
fun GoldenResultsInfo.toBenchmarksReport(): BenchmarksReport {
    val benchmarksSamples = goldenResults.map {
        BenchmarkResult(it.benchmarkName, BenchmarkResult.Status.PASSED,
                it.value, BenchmarkResult.metricFromString(it.metric)!!, it.value, 1, 0)
    }
    val compiler = Compiler(Compiler.Backend(Compiler.BackendType.NATIVE, "golden", emptyList()), "golden")
    val environment = Environment(Environment.Machine("golden", "golden"), Environment.JDKInstance("golden", "golden"))
    return BenchmarksReport(environment,
            benchmarksSamples, compiler)
}

// Build information provided from request.
data class TCBuildInfo(val buildNumber: String, val branch: String, val startTime: String,
                       val finishTime: String)

data class BuildRegister(val buildId: String, val teamCityUser: String, val teamCityPassword: String,
                         val bundleSize: String?, val fileWithResult: String) {
    companion object {
        fun create(json: String): BuildRegister {
            val requestDetails = JSON.parse<BuildRegister>(json)
            // Parse method doesn't create real instance with all methods. So create it by hands.
            return BuildRegister(requestDetails.buildId, requestDetails.teamCityUser, requestDetails.teamCityPassword,
                    requestDetails.bundleSize, requestDetails.fileWithResult)
        }
    }

    private val teamCityBuildUrl: String by lazy { "builds/id:$buildId" }

    val changesListUrl: String by lazy {
        "changes/?locator=build:id:$buildId"
    }

    val teamCityArtifactsUrl: String by lazy { "builds/id:$buildId/artifacts/content/$fileWithResult" }

    fun sendTeamCityRequest(url: String, json: Boolean = false) =
            UrlNetworkConnector(teamCityUrl).sendRequest(RequestMethod.GET, url, teamCityUser, teamCityPassword, json)

    fun getBranchName(project: String): Promise<String> {
        val url = "builds?locator=id:$buildId&fields=build(revisions(revision(vcsBranchName,vcs-root-instance)))"
        var branch: String? = null
        return sendTeamCityRequest(url, true).then { response ->
            val data = JsonTreeParser.parse(response).jsonObject
            data.getArray("build").forEach {
                (it as JsonObject).getObject("revisions").getArray("revision").forEach {
                    val currentBranch = (it as JsonObject).getPrimitive("vcsBranchName").content.removePrefix("refs/heads/")
                    val currentProject = (it as JsonObject).getObject("vcs-root-instance").getPrimitive("name").content
                    if (project == currentProject) {
                        branch = currentBranch
                    }
                    return@forEach
                }
            }
            branch ?: error("No project $project can be found in build $buildId")
        }
    }


    private fun format(timeValue: Int): String =
            if (timeValue < 10) "0$timeValue" else "$timeValue"

    fun getBuildInformation(): Promise<TCBuildInfo> {
        return Promise.all(arrayOf(sendTeamCityRequest("$teamCityBuildUrl/number"),
                getBranchName("Kotlin Native"),
                sendTeamCityRequest("$teamCityBuildUrl/startDate"))).then { results ->
            val (buildNumber, branch, startTime) = results
            val currentTime = Date()
            val timeZone = currentTime.getTimezoneOffset() / -60    // Convert to hours.
            // Get finish time as current time, because buid on TeamCity isn't finished.
            val finishTime = "${format(currentTime.getUTCFullYear())}" +
                    "${format(currentTime.getUTCMonth() + 1)}" +
                    "${format(currentTime.getUTCDate())}" +
                    "T${format(currentTime.getUTCHours())}" +
                    "${format(currentTime.getUTCMinutes())}" +
                    "${format(currentTime.getUTCSeconds())}" +
                    "${if (timeZone > 0) "+" else "-"}${format(timeZone)}${format(0)}"
            TCBuildInfo(buildNumber, branch, startTime, finishTime)
        }
    }
}

// Get builds numbers in right order.
internal fun <T> orderedValues(values: List<T>, buildElement: (T) -> String = { it -> it.toString() },
                      skipMilestones: Boolean = false) =
        values.sortedWith(
                compareBy({ buildElement(it).substringBefore(".").toInt() },
                        { buildElement(it).substringAfter(".").substringBefore("-").toDouble() },
                        {
                            if (skipMilestones) 0
                            else if (buildElement(it).substringAfter("-").startsWith("M"))
                                buildElement(it).substringAfter("M").substringBefore("-").toInt()
                            else
                                Int.MAX_VALUE
                        },
                        { buildElement(it).substringAfterLast("-").toDouble() }
                )
        )

// ElasticSearch connector for work with custom instance.
internal val localHostElasticConnector = UrlNetworkConnector("http://localhost", 9200)
// ElasticSearch connector for work with AWS instance.
internal val awsElasticConnector = AWSNetworkConnector()
internal val networkConnector = awsElasticConnector

fun urlParameterToBaseFormat(value: dynamic) =
        value.toString().replace("_", " ")

// Routing of requests to current server.
fun router() {
    val express = require("express")
    val router = express.Router()
    val connector = ElasticSearchConnector(networkConnector)
    val benchmarksDispatcher = BenchmarksIndexesDispatcher(connector, "env.machine.os",
            listOf("Linux", "Mac OS X", "Windows 10")
    )
    val goldenIndex = GoldenResultsIndex(connector)
    val buildInfoIndex = BuildInfoIndex(connector)

    router.get("/createMapping") { _, response ->
        buildInfoIndex.createMapping().then { _ ->
            response.sendStatus(200)
        }.catch { _ ->
            response.sendStatus(400)
        }
    }

    // Get consistent build information in cases of rerunning the same build.
    suspend fun getConsistentBuildInfo(buildInfoInstance: BuildInfo, reports: List<BenchmarksReport>,
                                       rerunNumber: Int = 1): BuildInfo {
        var currentBuildInfo = buildInfoInstance
        if (buildExists(currentBuildInfo, buildInfoIndex)) {
            // Check if benchmarks aren't repeated.
            val existingBecnhmarks = benchmarksDispatcher.getBenchmarksList(currentBuildInfo.buildNumber,
                    currentBuildInfo.agentInfo).await()
            val benchmarksToRegister = reports.map { it.benchmarks.keys }.flatten()
            if (existingBecnhmarks.toTypedArray().intersect(benchmarksToRegister).isNotEmpty()) {
                // Build was rerun.
                val buildNumber = "${currentBuildInfo.buildNumber}.$rerunNumber"
                currentBuildInfo = BuildInfo(buildNumber, currentBuildInfo.startTime, currentBuildInfo.endTime,
                        currentBuildInfo.commitsList, currentBuildInfo.branch, currentBuildInfo.agentInfo)
                return getConsistentBuildInfo(currentBuildInfo, reports, rerunNumber + 1)
            }
        }
        return currentBuildInfo
    }

    // Register build on Artifactory.
    router.post("/register") { request, response ->
        val register = BuildRegister.create(JSON.stringify(request.body))

        // Get information from TeamCity.
        register.getBuildInformation().then { buildInfo ->
            register.sendTeamCityRequest(register.changesListUrl, true).then { changes ->
                val commitsList = CommitsList(JsonTreeParser.parse(changes))
                // Get artifact.
                val content  = if(register.fileWithResult.contains("/"))
                    UrlNetworkConnector(artifactoryUrl).sendRequest(RequestMethod.GET, register.fileWithResult)
                else register.sendTeamCityRequest(register.teamCityArtifactsUrl)
                content.then { resultsContent ->
                    launch {
                        val reportData = JsonTreeParser.parse(resultsContent)
                        val reports = if (reportData is JsonArray) {
                            reportData.map { BenchmarksReport.create(it as JsonObject) }
                        } else listOf(BenchmarksReport.create(reportData as JsonObject))
                        val goldenResultPromise = getGoldenResults(goldenIndex)
                        val goldenResults = goldenResultPromise.await()
                        // Register build information.
                        var buildInfoInstance = getConsistentBuildInfo(
                                BuildInfo(buildInfo.buildNumber, buildInfo.startTime, buildInfo.finishTime,
                                        commitsList, buildInfo.branch, reports[0].env.machine.os),
                                reports
                        )
                        if (register.bundleSize != null) {
                            // Add bundle size.
                            val bundleSizeBenchmark = BenchmarkResult("KotlinNative",
                                    BenchmarkResult.Status.PASSED, register.bundleSize.toDouble(),
                                    BenchmarkResult.Metric.BUNDLE_SIZE, 0.0, 1, 0)
                            val bundleSizeReport = BenchmarksReport(reports[0].env,
                                    listOf(bundleSizeBenchmark), reports[0].compiler)
                            bundleSizeReport.buildNumber = buildInfoInstance.buildNumber
                            benchmarksDispatcher.insert(bundleSizeReport, reports[0].env.machine.os).then { _ ->
                                println("[BUNDLE] Success insert ${buildInfoInstance.buildNumber}")
                            }.catch { errorResponse ->
                                println("Failed to insert data for build")
                                println(errorResponse)
                            }
                        }
                        val insertResults = reports.map {
                            val benchmarksReport = SummaryBenchmarksReport(it).getBenchmarksReport()
                                    .normalizeBenchmarksSet(goldenResults)
                            benchmarksReport.buildNumber = buildInfoInstance.buildNumber
                            // Save results in database.
                            benchmarksDispatcher.insert(benchmarksReport, benchmarksReport.env.machine.os)
                        }
                        if (!buildExists(buildInfoInstance, buildInfoIndex)) {
                            buildInfoIndex.insert(buildInfoInstance).then { _ ->
                                println("Success insert build information for ${buildInfoInstance.buildNumber}")
                            }.catch {
                                response.sendStatus(400)
                            }
                        }
                        Promise.all(insertResults.toTypedArray()).then { _ ->
                            response.sendStatus(200)
                        }.catch {
                            response.sendStatus(400)
                        }
                    }
                }
            }
        }
    }

    // Register golden results to normalize on Artifactory.
    router.post("/registerGolden", { request, response ->
        val goldenResultsInfo: GoldenResultsInfo = JSON.parse<GoldenResultsInfo>(JSON.stringify(request.body))
        val goldenReport = goldenResultsInfo.toBenchmarksReport()
        goldenIndex.insert(goldenReport).then { _ ->
            response.sendStatus(200)
        }.catch {
            response.sendStatus(400)
        }
    })

    // Get builds description with additional information.
    router.get("/buildsDesc/:target", { request, response ->
        CachableResponseDispatcher.getResponse(request, response) { success, reject ->
            val target = request.params.target.toString().replace('_', ' ')

            var branch: String? = null
            var type: String? = null
            var buildsCountToShow = 200
            var beforeDate: String? = null
            var afterDate: String? = null
            if (request.query != undefined) {
                if (request.query.branch != undefined) {
                    branch = request.query.branch
                }
                if (request.query.type != undefined) {
                    type = request.query.type
                }
                if (request.query.count != undefined) {
                    buildsCountToShow = request.query.count.toString().toInt()
                }
                if (request.query.before != undefined) {
                    beforeDate = decodeURIComponent(request.query.before)
                }
                if (request.query.after != undefined) {
                    afterDate = decodeURIComponent(request.query.after)
                }
            }

            getBuildsInfo(type, branch, target, buildsCountToShow, buildInfoIndex, beforeDate, afterDate)
                    .then { buildsInfo ->
                val buildNumbers = buildsInfo.map { it.buildNumber }
                // Get number of failed benchmarks for each build.
                benchmarksDispatcher.getFailuresNumber(target, buildNumbers).then { failures ->
                    success(orderedValues(buildsInfo, { it -> it.buildNumber }, branch == "master").map {
                        Build(it.buildNumber, it.startTime, it.endTime, it.branch,
                                it.commitsList.serializeFields(), failures[it.buildNumber] ?: 0)
                    })
                }.catch { errorResponse ->
                    println("Error during getting failures numbers")
                    println(errorResponse)
                    reject()
                }
            }.catch {
                reject()
            }
        }
    })

    // Get values of current metric.
    router.get("/metricValue/:target/:metric", { request, response ->
        CachableResponseDispatcher.getResponse(request, response) { success, reject ->
            val metric = request.params.metric
            val target = request.params.target.toString().replace('_', ' ')
            var samples: List<String> = emptyList()
            var aggregation = "geomean"
            var normalize = false
            var branch: String? = null
            var type: String? = null
            var excludeNames: List<String> = emptyList()
            var buildsCountToShow = 200
            var beforeDate: String? = null
            var afterDate: String? = null

            // Parse parameters from request if it exists.
            if (request.query != undefined) {
                if (request.query.samples != undefined) {
                    samples = request.query.samples.toString().split(",").map { it.trim() }
                }
                if (request.query.agr != undefined) {
                    aggregation = request.query.agr.toString()
                }
                if (request.query.normalize != undefined) {
                    normalize = true
                }
                if (request.query.branch != undefined) {
                    branch = request.query.branch
                }
                if (request.query.type != undefined) {
                    type = request.query.type
                }
                if (request.query.exclude != undefined) {
                    excludeNames = request.query.exclude.toString().split(",").map { it.trim() }
                }
                if (request.query.count != undefined) {
                    buildsCountToShow = request.query.count.toString().toInt()
                }
                if (request.query.before != undefined) {
                    beforeDate = decodeURIComponent(request.query.before)
                }
                if (request.query.after != undefined) {
                    afterDate = decodeURIComponent(request.query.after)
                }
            }

            getBuildsNumbers(type, branch, target, buildsCountToShow, buildInfoIndex, beforeDate, afterDate).then { buildNumbers ->
                if (aggregation == "geomean") {
                    // Get geometric mean for samples.
                    benchmarksDispatcher.getGeometricMean(metric, target, buildNumbers, normalize,
                            excludeNames).then { geoMeansValues ->
                        success(orderedValues(geoMeansValues, { it -> it.first }, branch == "master"))
                    }.catch { errorResponse ->
                        println("Error during getting geometric mean")
                        println(errorResponse)
                        reject()
                    }
                } else {
                    benchmarksDispatcher.getSamples(metric, target, samples, buildsCountToShow, buildNumbers, normalize)
                            .then { geoMeansValues ->
                        success(orderedValues(geoMeansValues, { it -> it.first }, branch == "master"))
                    }.catch {
                        println("Error during getting samples")
                        reject()
                    }
                }
            }.catch {
                println("Error during getting builds information")
                reject()
            }
        }
    })

    // Get branches for [target].
    router.get("/branches", { request, response ->
        CachableResponseDispatcher.getResponse(request, response) { success, reject ->
            distinctValues("branch", buildInfoIndex).then { results ->
                success(results)
            }.catch { errorMessage ->
                error(errorMessage.message ?: "Failed getting branches list.")
                reject()
            }
        }
    })

    // Get build numbers for [target].
    router.get("/buildsNumbers/:target", { request, response ->
        CachableResponseDispatcher.getResponse(request, response) { success, reject ->
            distinctValues("buildNumber", buildInfoIndex).then { results ->
                success(results)
            }.catch { errorMessage ->
                println(errorMessage.message ?: "Failed getting branches list.")
                reject()
            }
        }
    })

    // Conert data and migrate it from Artifactory to DB.
    router.get("/migrate/:target", { request, response ->
        val target = urlParameterToBaseFormat(request.params.target)
        val targetPathName = target.replace(" ", "")
        var buildNumber: String? = null
        if (request.query != undefined) {
            if (request.query.buildNumber != undefined) {
                buildNumber = request.query.buildNumber
                buildNumber = request.query.buildNumber
            }
        }
        getBuildsInfoFromArtifactory(targetPathName).then { buildInfo ->
            launch {
                val buildsDescription = buildInfo.lines().drop(1)
                var shouldConvert = buildNumber?.let { false } ?: true
                val goldenResultPromise = getGoldenResults(goldenIndex)
                val goldenResults = goldenResultPromise.await()
                val buildsSet = mutableSetOf<String>()
                buildsDescription.forEach {
                    if (!it.isEmpty()) {
                        val currentBuildNumber = it.substringBefore(',')
                        if (!"\\d+(\\.\\d+)+(-M\\d)?-\\w+-\\d+(\\.\\d+)?".toRegex().matches(currentBuildNumber)) {
                            error("Build number $currentBuildNumber differs from expected format. File with data for " +
                                    "target $target could be corrupted.")
                        }
                        if (!shouldConvert && buildNumber != null && buildNumber == currentBuildNumber) {
                            shouldConvert = true
                        }
                        if (shouldConvert) {
                            // Save data from Artifactory into database.
                            val artifactoryUrlConnector = UrlNetworkConnector(artifactoryUrl)
                            val fileName = "nativeReport.json"
                            val accessFileUrl = "$targetPathName/$currentBuildNumber/$fileName"
                            val extrenalFileName = if (target == "Linux") "externalReport.json" else "spaceFrameworkReport.json"
                            val accessExternalFileUrl = "$targetPathName/$currentBuildNumber/$extrenalFileName"
                            val infoParts = it.split(", ")
                            if ((infoParts[3] == "master" || "eap" in currentBuildNumber || "release" in currentBuildNumber) &&
                                    currentBuildNumber !in buildsSet) {
                                try {
                                    buildsSet.add(currentBuildNumber)
                                    val jsonReport = artifactoryUrlConnector.sendRequest(RequestMethod.GET, accessFileUrl).await()
                                    var reports = convert(jsonReport, currentBuildNumber, target)
                                    val buildInfoRecord = BuildInfo(currentBuildNumber, infoParts[1], infoParts[2],
                                            CommitsList.parse(infoParts[4]), infoParts[3], target)

                                    val externalJsonReport = artifactoryUrlConnector.sendOptionalRequest(RequestMethod.GET, accessExternalFileUrl)
                                            .await()
                                    buildInfoIndex.insert(buildInfoRecord).then { _ ->
                                        println("[BUILD INFO] Success insert build number ${buildInfoRecord.buildNumber}")
                                        externalJsonReport?.let {
                                            var externalReports = convert(externalJsonReport.replace("circlet_iosX64", "SpaceFramework_iosX64"),
                                                    currentBuildNumber, target)
                                            externalReports.forEach { externalReport ->
                                                val extrenalAdditionalReport = SummaryBenchmarksReport(externalReport)
                                                        .getBenchmarksReport().normalizeBenchmarksSet(goldenResults)
                                                extrenalAdditionalReport.buildNumber = currentBuildNumber
                                                benchmarksDispatcher.insert(extrenalAdditionalReport, target).then { _ ->
                                                    println("[External] Success insert ${buildInfoRecord.buildNumber}")
                                                }.catch { errorResponse ->
                                                    println("Failed to insert data for build")
                                                    println(errorResponse)
                                                }
                                            }
                                        }

                                        val bundleSize = if (infoParts[10] != "-") infoParts[10] else null
                                        if (bundleSize != null) {
                                            // Add bundle size.
                                            val bundleSizeBenchmark = BenchmarkResult("KotlinNative",
                                                    BenchmarkResult.Status.PASSED, bundleSize.toDouble(),
                                                    BenchmarkResult.Metric.BUNDLE_SIZE, 0.0, 1, 0)
                                            val bundleSizeReport = BenchmarksReport(reports[0].env,
                                                    listOf(bundleSizeBenchmark), reports[0].compiler)
                                            bundleSizeReport.buildNumber = currentBuildNumber
                                            benchmarksDispatcher.insert(bundleSizeReport, target).then { _ ->
                                                println("[BUNDLE] Success insert ${buildInfoRecord.buildNumber}")
                                            }.catch { errorResponse ->
                                                println("Failed to insert data for build")
                                                println(errorResponse)
                                            }
                                        }

                                        reports.forEach { report ->
                                            val summaryReport = SummaryBenchmarksReport(report).getBenchmarksReport()
                                                    .normalizeBenchmarksSet(goldenResults)
                                            summaryReport.buildNumber = currentBuildNumber
                                            // Save results in database.
                                            benchmarksDispatcher.insert(summaryReport, target).then { _ ->
                                                println("Success insert ${buildInfoRecord.buildNumber}")
                                            }.catch { errorResponse ->
                                                println("Failed to insert data for build")
                                                println(errorResponse.message)
                                            }
                                        }


                                    }.catch { errorResponse ->
                                        println("Failed to insert data for build")
                                        println(errorResponse)
                                    }
                                } catch (e: Exception) {
                                    println(e)
                                }
                            }
                        }
                    }
                }
            }
            response.sendStatus(200)
        }.catch {
            response.sendStatus(400)
        }
    })

    router.get("/delete/:target", { request, response ->
        val target = urlParameterToBaseFormat(request.params.target)
        var buildNumber: String? = null
        if (request.query != undefined) {
            if (request.query.buildNumber != undefined) {
                buildNumber = request.query.buildNumber
            }
        }
        benchmarksDispatcher.deleteBenchmarks(target, buildNumber).then {
            deleteBuildInfo(target, buildInfoIndex, buildNumber).then {
                response.sendStatus(200)
            }.catch {
                response.sendStatus(400)
            }
        }.catch {
            response.sendStatus(400)
        }
    })

    router.get("/report/:target/:buildNumber", { request, response ->
        val target = urlParameterToBaseFormat(request.params.target)
        val buildNumber = request.params.buildNumber.toString()
        benchmarksDispatcher.getBenchmarksReports(buildNumber, target).then { reports ->
            response.send(reports.joinToString(", ", "[", "]"))
        }.catch {
            response.sendStatus(400)
        }
    })

    router.get("/clear", { _, response ->
        CachableResponseDispatcher.clear()
        response.sendStatus(200)
    })

    // Main page.
    router.get("/", { _, response ->
        response.render("index")
    })

    return router
}

fun getBuildsInfoFromArtifactory(target: String): Promise<String> {
    val buildsFileName = "buildsSummary.csv"
    val artifactoryBuildsDirectory = "builds"
    return UrlNetworkConnector(artifactoryUrl).sendRequest(RequestMethod.GET,
            "$artifactoryBuildsDirectory/$target/$buildsFileName")
}

fun BenchmarksReport.normalizeBenchmarksSet(dataForNormalization: Map<String, List<BenchmarkResult>>): BenchmarksReport {
    val resultBenchmarksList = benchmarks.map { benchmarksList ->
        benchmarksList.value.map {
            NormalizedMeanVarianceBenchmark(it.name, it.status, it.score, it.metric,
                    it.runtimeInUs, it.repeat, it.warmup, (it as MeanVarianceBenchmark).variance,
                    dataForNormalization[benchmarksList.key]?.get(0)?.score?.let { golden -> it.score / golden } ?: 0.0)
        }
    }.flatten()
    return BenchmarksReport(env, resultBenchmarksList, compiler)
}