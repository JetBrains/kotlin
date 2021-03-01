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

fun <T> Iterable<T>.isEmpty() = count() == 0
fun <T> Iterable<T>.isNotEmpty() = !isEmpty()

inline fun <T: Any> T?.str(block: (T) -> String): String =
        if (this != null) block(this)
        else ""

// Dispatcher to create and control benchmarks indexes separated by some feature.
// Feature can be choosen as often used as filtering entity in case there is no need in separate indexes.
// Default behaviour of dispatcher is working with one index (case when separating isn't needed).
class BenchmarksIndexesDispatcher(connector: ElasticSearchConnector, val feature: String,
                                  featureValues: Iterable<String> = emptyList()) {
    // Becnhmarks indexes to work with in case of existing feature values.
    private val benchmarksIndexes =
            if (featureValues.isNotEmpty())
                featureValues.map { it to BenchmarksIndex("benchmarks_${it.replace(" ", "_").toLowerCase()}", connector) }
                        .toMap()
            else emptyMap()

    // Single benchmark index.
    private val benchmarksSingleInstance =
            if (featureValues.isEmpty()) BenchmarksIndex("benchmarks", connector) else null

    // Get right index in ES.
    private fun getIndex(featureValue: String = "") =
            benchmarksSingleInstance ?: benchmarksIndexes[featureValue]
            ?: error("Used wrong feature value $featureValue. Indexes are separated using next values: ${benchmarksIndexes.keys}")

    // Used filter to get data with needed feature value.
    var featureFilter: ((String) -> String)? = null

    // Get benchmark reports corresponding to needed build number.
    fun getBenchmarksReports(buildNumber: String, featureValue: String): Promise<List<String>> {
        val queryDescription = """
            {
                "size": 1000,
                "query": {
                    "bool": {
                        "must": [ 
                            { "match": { "buildNumber": "$buildNumber" } }
                        ]
                    }
                }
            }
            """

        return getIndex(featureValue).search(queryDescription, listOf("hits.hits._source")).then { responseString ->
            val dbResponse = JsonTreeParser.parse(responseString).jsonObject
            dbResponse.getObjectOrNull("hits")?.getArrayOrNull("hits")?.let { results ->
                results.map {
                    val element = it as JsonObject
                    element.getObject("_source").toString()
                }
            } ?: emptyList()
        }
    }

    // Get benchmarkes names corresponding to needed build number.
    fun getBenchmarksList(buildNumber: String, featureValue: String): Promise<List<String>> {
        return getBenchmarksReports(buildNumber, featureValue).then { reports ->
            reports.map {
                val dbResponse = JsonTreeParser.parse(it).jsonObject
                parseBenchmarksArray(dbResponse.getArray("benchmarks"))
                        .map { it.name }
            }.flatten()
        }
    }

    // Delete benchmarks from database.
    fun deleteBenchmarks(featureValue: String, buildNumber: String? = null): Promise<String> {
        // Delete all or for choosen build number.
        val matchQuery = buildNumber?.let {
            """"match": { "buildNumber": "$it" }"""
        } ?: """"match_all": {}"""

        val queryDescription = """
            {
                "query": {
                    $matchQuery
                }
            }
        """.trimIndent()
        return getIndex(featureValue).delete(queryDescription)
    }

    // Get benchmarks values of needed metric for choosen build number.
    fun getSamples(metricName: String, featureValue: String = "", samples: List<String>, buildsCountToShow: Int,
                   buildNumbers: Iterable<String>? = null,
                   normalize: Boolean = false): Promise<List<Pair<String, Array<Double?>>>> {
        val queryDescription = """
            {
                "_source": ["buildNumber"],
                "size": ${samples.size * buildsCountToShow},
                "query": {
                    "bool": {
                        "must": [ 
                            ${buildNumbers.str { builds ->
            """
                            { "terms" : { "buildNumber" : [${builds.map { "\"$it\"" }.joinToString()}] } },""" }
        }
                            ${featureFilter.str { "${it(featureValue)}," } }
                            {"nested" : {
                                "path" : "benchmarks",
                                "query" : {
                                    "bool": {
                                        "must": [
                                            { "match": { "benchmarks.metric": "$metricName" } },
                                            { "terms": { "benchmarks.name": [${samples.map { "\"${it.toLowerCase()}\"" }.joinToString()}] }}
                                        ]
                                    }  
                                }, "inner_hits": {
                                    "size": ${samples.size}, 
                                    "_source": ["benchmarks.name", 
                                    "benchmarks.${if (normalize) "normalizedScore" else "score"}"]
                                }    
                            }
                        }
                    ]
                }
            } 
        }"""

        return getIndex(featureValue).search(queryDescription, listOf("hits.hits._source", "hits.hits.inner_hits"))
                .then { responseString ->
                    val dbResponse = JsonTreeParser.parse(responseString).jsonObject
                    val results = dbResponse.getObjectOrNull("hits")?.getArrayOrNull("hits")
                            ?: error("Wrong response:\n$responseString")
                    // Get indexes for provided samples.
                    val indexesMap = samples.mapIndexed { index, it -> it to index }.toMap()
                    val valuesMap = buildNumbers?.map {
                        it to arrayOfNulls<Double?>(samples.size)
                    }?.toMap()?.toMutableMap() ?: mutableMapOf<String, Array<Double?>>()
                    // Parse and save values in requested order.
                    results.forEach {
                        val element = it as JsonObject
                        val build = element.getObject("_source").getPrimitive("buildNumber").content
                        buildNumbers?.let { valuesMap.getOrPut(build) { arrayOfNulls<Double?>(samples.size) } }
                        element
                                .getObject("inner_hits")
                                .getObject("benchmarks")
                                .getObject("hits")
                                .getArray("hits").forEach {
                                    val source = (it as JsonObject).getObject("_source")
                                    valuesMap[build]!![indexesMap[source.getPrimitive("name").content]!!] =
                                            source.getPrimitive(if (normalize) "normalizedScore" else "score").double
                                }

                    }
                    valuesMap.toList()
                }
    }

    fun insert(data: JsonSerializable, featureValue: String = "") =
            getIndex(featureValue).insert(data)

    fun delete(data: String, featureValue: String = "") =
            getIndex(featureValue).delete(data)

    // Get failures number happned during build.
    fun getFailuresNumber(featureValue: String = "", buildNumbers: Iterable<String>? = null): Promise<Map<String, Int>> {
        val queryDescription = """ 
            {
                "_source": false,
                ${featureFilter.str {
            """
                "query": {
                    "bool": {
                        "must": [ ${it(featureValue)} ]
                    }
                }, """
        } }
                ${buildNumbers.str { builds ->
            """
                "aggs" : {
                    "builds": {
                        "filters" : { 
                            "filters": { 
                                ${builds.map { "\"$it\": { \"match\" : { \"buildNumber\" : \"$it\" }}" }
                    .joinToString(",\n")}
                            }
                        },"""
        } }
                    "aggs" : {
                        "metric_build" : {
                            "nested" : {
                                "path" : "benchmarks"
                            },
                            "aggs" : {
                                "metric_samples": {
                                    "filters" : { 
                                        "filters": { "samples": { "match": { "benchmarks.status": "FAILED" } } }
                                    },
                                    "aggs" : {
                                        "failed_count": {
                                            "value_count": {
                                                "field" : "benchmarks.score"
                                            }
                                        }
                                    }
                                }
                            }
                    ${buildNumbers.str {
            """ }
                }"""
        } }
        }
    }
}
"""
        return getIndex(featureValue).search(queryDescription, listOf("aggregations")).then { responseString ->
            val dbResponse = JsonTreeParser.parse(responseString).jsonObject
            val aggregations = dbResponse.getObjectOrNull("aggregations") ?: error("Wrong response:\n$responseString")
            buildNumbers?.let {
                // Get failed number for each provided build.
                val buckets = aggregations
                        .getObjectOrNull("builds")
                        ?.getObjectOrNull("buckets")
                        ?: error("Wrong response:\n$responseString")
                buildNumbers.map {
                    it to buckets
                            .getObject(it)
                            .getObject("metric_build")
                            .getObject("metric_samples")
                            .getObject("buckets")
                            .getObject("samples")
                            .getObject("failed_count")
                            .getPrimitive("value")
                            .int
                }.toMap()
            } ?: listOf("golden" to aggregations
                    .getObject("metric_build")
                    .getObject("metric_samples")
                    .getObject("buckets")
                    .getObject("samples")
                    .getObject("failed_count")
                    .getPrimitive("value")
                    .int
            ).toMap()
        }
    }

    // Get geometric mean for benchmarks values of needed metric.
    fun getGeometricMean(metricName: String, featureValue: String = "",
                         buildNumbers: Iterable<String>? = null, normalize: Boolean = false,
                         excludeNames: List<String> = emptyList()): Promise<List<Pair<String, List<Double?>>>> {

        // Filter only with metric or also with names.
        val filterBenchmarks = if (excludeNames.isEmpty())
            """
            "match": { "benchmarks.metric": "$metricName" }
            """
        else """
            "bool": { 
                "must": { "match": { "benchmarks.metric": "$metricName" } },
                "must_not": [ ${excludeNames.map { """{ "match_phrase" : { "benchmarks.name" : "$it" } }"""}.joinToString() } ]
            }
        """.trimIndent()
        val queryDescription = """
            {
                "_source": false,
                ${featureFilter.str {
            """
                "query": {
                    "bool": {
                        "must": [ ${it(featureValue)} ]
                    }
                }, """
        } }
                ${buildNumbers.str { builds ->
            """
                "aggs" : {
                    "builds": {
                        "filters" : { 
                            "filters": { 
                                ${builds.map { "\"$it\": { \"match\" : { \"buildNumber\" : \"$it\" }}" }
                    .joinToString(",\n")}
                            }
                        },"""
        } }
                    "aggs" : {
                        "metric_build" : {
                            "nested" : {
                                "path" : "benchmarks"
                            },
                            "aggs" : {
                                "metric_samples": {
                                    "filters" : { 
                                        "filters": { "samples": { $filterBenchmarks } }
                                    },
                                    "aggs" : {
                                        "sum_log_x": {
                                            "sum": {
                                                "field" : "benchmarks.${if (normalize) "normalizedScore" else "score"}",
                                                "script" : {
                                                    "source": "if (_value == 0) { 0.0 } else { Math.log(_value) }"
                                                }
                                            }
                                        },
                                        "geom_mean": {
                                            "bucket_script": {
                                                "buckets_path": {
                                                    "sum_log_x": "sum_log_x",
                                                    "x_cnt": "_count"
                                                },
                                                "script": "Math.exp(params.sum_log_x/params.x_cnt)"
                                            }
                                        }
                                    }
                                }
                            }
                        
                           ${buildNumbers.str {
            """ }
                        }"""
        } }
                    }
                }
            }
        """

        return getIndex(featureValue).search(queryDescription, listOf("aggregations")).then { responseString ->
            val dbResponse = JsonTreeParser.parse(responseString).jsonObject
            val aggregations = dbResponse.getObjectOrNull("aggregations") ?: error("Wrong response:\n$responseString")
            buildNumbers?.let {
                val buckets = aggregations
                        .getObjectOrNull("builds")
                        ?.getObjectOrNull("buckets")
                        ?: error("Wrong response:\n$responseString")
                buildNumbers.map {
                    it to listOf(buckets
                            .getObject(it)
                            .getObject("metric_build")
                            .getObject("metric_samples")
                            .getObject("buckets")
                            .getObject("samples")
                            .getObjectOrNull("geom_mean")
                            ?.getPrimitive("value")
                            ?.double
                    )
                }
            } ?: listOf("golden" to listOf(aggregations
                    .getObject("metric_build")
                    .getObject("metric_samples")
                    .getObject("buckets")
                    .getObject("samples")
                    .getObjectOrNull("geom_mean")
                    ?.getPrimitive("value")
                    ?.double
                )
            )
        }
    }
}