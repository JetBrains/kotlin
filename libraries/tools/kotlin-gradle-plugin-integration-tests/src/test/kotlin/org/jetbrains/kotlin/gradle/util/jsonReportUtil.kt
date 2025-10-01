/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.stream.JsonReader
import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Path
import kotlin.io.path.bufferedReader

data class BuildOperationRecordImpl(
    override val path: String,
    override val classFqName: String,
    override val isFromKotlinPlugin: Boolean,
    override val startTimeMs: Long, // Measured by System.currentTimeMillis(),
    override val totalTimeMs: Long,
    override val buildMetrics: BuildMetrics<BuildTimeMetric, BuildPerformanceMetric>,
    override val didWork: Boolean,
    override val skipMessage: String?,
    override val icLogLines: List<String>,
    //taskRecords
    val kotlinLanguageVersion: KotlinVersion?,
    val changedFiles: SourcesChanges? = null,
    val compilerArguments: List<String> = emptyList(),
    val statTags: Set<StatTag> = emptySet(),
) : BuildOperationRecord


//TODO: KT-66071 update deserialization.
// the `buildExecutionDataGson` variable from :kotlin-build-statistics project can't be used because of gson library shadowing
// for embedded compiler dependency. The returning type is org.jetbrains.kotlin.com.google.gson.Gson.
internal val buildExecutionDataGson = GsonBuilder()
    .registerTypeAdapter(File::class.java, object : JsonSerializer<File> {
        override fun serialize(src: File?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return src?.path?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
        }
    })
    .registerTypeAdapter(File::class.java, object : JsonDeserializer<File> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): File? {
            val path = context?.deserialize<String>(json, String::class.java)
            return path?.let { File(it) }
        }
    })
    .registerTypeAdapter(BuildOperationRecord::class.java, object : JsonDeserializer<BuildOperationRecord> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): BuildOperationRecord? {
            //workaround to read both TaskRecord and TransformRecord
            return context?.deserialize(json, BuildOperationRecordImpl::class.java)
        }
    }).registerTypeAdapter(SourcesChanges::class.java, object : JsonDeserializer<SourcesChanges> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext,
        ): SourcesChanges? {
            return null //ignore source changes right now
        }
    }).registerTypeAdapter(BuildPerformanceMetric::class.java, object : JsonDeserializer<BuildPerformanceMetric> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): BuildPerformanceMetric? {
            val metricName = json?.asJsonObject["name"]?.let { context?.deserialize<String>(it, String::class.java) } ?: return null
            val metric = getAllMetrics().firstOrNull { it.name == metricName }
            if (metric != null) return metric

            val parentMetricName =
                json?.asJsonObject["parent"]?.asJsonObject["name"]?.let { context?.deserialize<String>(it, String::class.java) }
            val parentMetric = allBuildTimeMetrics.firstOrNull { it.name == parentMetricName }

            return CustomBuildTimeMetric.createIfDoesNotExistAndReturn(name = metricName, parentMetric)
        }

    }).registerTypeAdapter(BuildTimeMetric::class.java, object : JsonDeserializer<BuildTimeMetric> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): BuildTimeMetric? {
            val metricName = json?.asJsonObject["name"]?.let { context?.deserialize<String>(it, String::class.java) } ?: return null
            val metric = allBuildTimeMetrics.firstOrNull { it.name == metricName }
            if (metric != null) return metric

            val parentMetricName =
                json?.asJsonObject["parent"]?.asJsonObject["name"]?.let { context?.deserialize<String>(it, String::class.java) }
            val parentMetric = allBuildTimeMetrics.firstOrNull { it.name == parentMetricName }

            return CustomBuildTimeMetric.createIfDoesNotExistAndReturn(name = metricName, parentMetric)
        }

    })
    .create()

internal fun readJsonReport(jsonReport: Path): BuildExecutionData {
    val buildExecutionData = jsonReport.bufferedReader().use {
        buildExecutionDataGson.fromJson(JsonReader(it), BuildExecutionData::class.java) as BuildExecutionData
    }
    return buildExecutionData
}
