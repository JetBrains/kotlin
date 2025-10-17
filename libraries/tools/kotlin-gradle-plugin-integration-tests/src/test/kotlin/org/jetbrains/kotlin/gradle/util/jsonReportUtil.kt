/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.stream.JsonReader
import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.statistics.StatTag
import org.jetbrains.kotlin.build.report.statistics.json.buildExecutionDataGson
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.data.BuildExecutionData
import org.jetbrains.kotlin.gradle.report.data.BuildOperationRecord
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

//KT-66071 update deserialization
val testBuildExecutionDataGson = buildExecutionDataGson
    .newBuilder()
    .registerTypeAdapter(BuildOperationRecord::class.java, object : JsonDeserializer<BuildOperationRecord> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): BuildOperationRecord? {
            //workaround to read both TaskRecord and TransformRecord
            return context?.deserialize(json, BuildOperationRecordImpl::class.java)
        }
    })
    .registerTypeAdapter(SourcesChanges::class.java, object : JsonDeserializer<SourcesChanges> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext,
        ): SourcesChanges? {
            return null //ignore source changes right now
        }
    })
    .create()

internal fun readJsonReport(jsonReport: Path): BuildExecutionData {
    val buildExecutionData = jsonReport.bufferedReader().use {
        testBuildExecutionDataGson.fromJson(JsonReader(it), BuildExecutionData::class.java) as BuildExecutionData
    }
    return buildExecutionData
}
