/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.database

import kotlin.js.Promise
import org.jetbrains.elastic.*
import org.jetbrains.utils.*
import org.jetbrains.report.json.*
import org.jetbrains.report.*

// Delete build information from ES index.
internal fun deleteBuildInfo(agentInfo: String, buildInfoIndex: ElasticSearchIndex,
                             buildNumber: String? = null): Promise<String> {
    val queryDescription = """
            {
                "query": {
                    "bool": {
                        "must": [ 
                            { "match": { "agentInfo": "$agentInfo" } }
                            ${buildNumber?.let {
        """,
                            {"match": { "buildNumber": "$it" }}
                            """
    } ?: ""}
                        ]
                    }
                }
            }
        """.trimIndent()
    return buildInfoIndex.delete(queryDescription)
}

// Get infromation about builds details from database.
internal fun getBuildsDescription(type: String?, branch: String?, agentInfo: String, buildInfoIndex: ElasticSearchIndex,
                                  onlyNumbers: Boolean = false): Promise<JsonArray> {
    val queryDescription = """
            {   "size": 10000,
                ${if (onlyNumbers) """"_source": ["buildNumber"],""" else ""}
                "sort": {"_id": "desc" },
                "query": {
                    "bool": {
                        "must": [ 
                            { "match": { "agentInfo": "$agentInfo" } }
                            ${type?.let {
        """,
                            { "regexp": { "buildNumber": { "value": "${if (it == "release")
            ".*eap.*|.*release.*|.*rc.*" else ".*dev.*"}" } } 
                            }
                            """
    } ?: ""} 
                            ${branch?.let {
        """,
                            {"match": { "branch": "$it" }}
                            """
    } ?: ""}
                        ]
                    }
                }
            }
        """.trimIndent()
    return buildInfoIndex.search(queryDescription, listOf("hits.hits._source")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        dbResponse.getObjectOrNull("hits")?.getArrayOrNull("hits") ?: error("Wrong response:\n$responseString")
    }
}

// Check if current build already exists.
suspend fun buildExists(buildInfo: BuildInfo, buildInfoIndex: ElasticSearchIndex): Boolean {
    val queryDescription = """
            {   "size": 1,
               "_source": ["buildNumber"],
                "query": {
                    "bool": {
                        "must": [ 
                            { "match": { "buildNumber": "${buildInfo.buildNumber}" } },
                            { "match": { "agentInfo": "${buildInfo.agentInfo}" } },
                            { "match": { "branch": "${buildInfo.branch}" } }
                        ]
                    }
                }
            }
        """.trimIndent()

    return buildInfoIndex.search(queryDescription, listOf("hits.total.value")).then { responseString ->
        val response = JsonTreeParser.parse(responseString).jsonObject
        val value = response.getObjectOrNull("hits")?.getObjectOrNull("total")?.getPrimitiveOrNull("value")?.content
                ?: error("Error response from ElasticSearch:\n$responseString")
        value.toInt() > 0
    }.await()
}

// Get builds numbers corresponding to machine and branch.
fun getBuildsNumbers(type: String?, branch: String?, agentInfo: String, buildInfoIndex: ElasticSearchIndex) =
        getBuildsDescription(type, branch, agentInfo, buildInfoIndex, true).then { responseArray ->
            responseArray.map { (it as JsonObject).getObject("_source").getPrimitive("buildNumber").content }
        }

// Get full builds information corresponding to machine and branch.
fun getBuildsInfo(type: String?, branch: String?, agentInfo: String, buildInfoIndex: ElasticSearchIndex) =
        getBuildsDescription(type, branch, agentInfo, buildInfoIndex).then { responseArray ->
            responseArray.map { BuildInfo.create((it as JsonObject).getObject("_source")) }
        }

// Get golden results from database.
fun getGoldenResults(goldenResultsIndex: GoldenResultsIndex): Promise<Map<String, List<BenchmarkResult>>> {
    return goldenResultsIndex.search("", listOf("hits.hits._source")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        dbResponse.getObjectOrNull("hits")?.getArrayOrNull("hits")?.map {
            val reportDescription = (it as JsonObject).getObject("_source")
            BenchmarksReport.create(reportDescription).benchmarks
        }?.reduce { acc, it -> acc + it } ?: error("Wrong format of response:\n $responseString")
    }
}

// Get distinct values for needed field from database.
fun distinctValues(field: String, index: ElasticSearchIndex): Promise<List<String>> {
    val queryDescription = """
            {
              "aggs": {
                    "unique": {"terms": {"field": "$field", "size": 1000}}
                }
            }
        """.trimIndent()
    return index.search(queryDescription, listOf("aggregations.unique.buckets")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        dbResponse.getObjectOrNull("aggregations")?.getObjectOrNull("unique")?.getArrayOrNull("buckets")
                ?.map { (it as JsonObject).getPrimitiveOrNull("key")?.content }?.filterNotNull()
                ?: error("Wrong response:\n$responseString")
    }
}