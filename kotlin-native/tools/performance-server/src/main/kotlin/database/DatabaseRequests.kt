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
                                  buildsCountToShow: Int, beforeDate: String?, afterDate: String?,
                                  onlyNumbers: Boolean = false): Promise<JsonArray> {
    val queryDescription = """
            {   "size": $buildsCountToShow,
                ${if (onlyNumbers) """"_source": ["buildNumber", "buildType"],""" else ""}
                "sort": {"startTime": "desc" },
                "query": {
                    "bool": {
                        ${type?.let {"""
                        "must": [
                            { "bool": {
                                "should": [
                                     { "regexp": { "buildNumber": { "value": "${if (it == "release")  
                                        ".*eap.*|.*release.*|.*rc.*" else ".*dev.*"}" } } 
                                     },
                                     { "match": { "buildType": "${it.toUpperCase()}" } }
                                 ]
                            }},
                            { "bool": {
                        
                        """} ?: ""}
                        "must": [ 
                            { "match": { "agentInfo": "$agentInfo" } }
                            ${beforeDate?.let {
        """,
                            { "range": { "startTime": { "lt": "$it" } } }
                            """
    } ?: ""} 
                            ${afterDate?.let {
        """,
                            { "range": { "startTime": { "gt": "$it" } } }
                            """
    } ?: ""}
                            ${branch?.let {
        """,
                            {"match": { "branch": "$it" }}
                            """
    } ?: ""}
                        ]
                        ${type?.let {"""
                            }}]
                        """}}
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
fun getBuildsNumbers(type: String?, branch: String?, agentInfo: String, buildsCountToShow: Int,
                     buildInfoIndex: ElasticSearchIndex, beforeDate: String? = null, afterDate: String? = null) =
        getBuildsDescription(type, branch, agentInfo, buildInfoIndex, buildsCountToShow, beforeDate, afterDate, true)
                .then { responseArray ->
            responseArray.map {
                val build = (it as JsonObject).getObject("_source")
                build.getPrimitiveOrNull("buildType")?.content to build.getPrimitive("buildNumber").content
            }
        }

// Get full builds information corresponding to machine and branch.
fun getBuildsInfo(type: String?, branch: String?, agentInfo: String, buildsCountToShow: Int,
                  buildInfoIndex: ElasticSearchIndex, beforeDate: String? = null,
                  afterDate: String? = null) =
        getBuildsDescription(type, branch, agentInfo, buildInfoIndex, buildsCountToShow, beforeDate, afterDate).then { responseArray ->
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

// Get list of unstable benchmarks from database.
fun getUnstableResults(goldenResultsIndex: GoldenResultsIndex): Promise<List<String>> {
    val queryDescription = """
        {
          "_source": ["env"],
          "query": {
            "nested" : {
              "path" : "benchmarks",
              "query" : {
                "match": { "benchmarks.unstable": true } 
              },
              "inner_hits": {
                "size": 100, 
                "_source": ["benchmarks.name"]
              }    
            }                      
          } 
        }
    """.trimIndent()

    return goldenResultsIndex.search(queryDescription, listOf("hits.hits.inner_hits")).then { responseString ->
        val dbResponse = JsonTreeParser.parse(responseString).jsonObject
        val results = dbResponse.getObjectOrNull("hits")?.getArrayOrNull("hits")
                ?: error("Wrong response:\n$responseString")
        results.getObjectOrNull(0)?.let {
            it
                    .getObject("inner_hits")
                    .getObject("benchmarks")
                    .getObject("hits")
                    .getArray("hits").map {
                        (it as JsonObject).getObject("_source").getPrimitive("name").content
                    }
        } ?: listOf<String>()
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