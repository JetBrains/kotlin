/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.util

import com.google.gson.*
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
    override val buildMetrics: BuildMetrics<GradleBuildTime, GradleBuildPerformanceMetric>,
    override val didWork: Boolean,
    override val skipMessage: String?,
    override val icLogLines: List<String>,
    //taskRecords
    val kotlinLanguageVersion: KotlinVersion?,
    val changedFiles: SourcesChanges? = null,
    val compilerArguments: List<String> = emptyList(),
    val statTags: Set<StatTag> = emptySet(),
) : BuildOperationRecord


internal fun readJsonReport(jsonReport: Path): BuildExecutionData {
    //TODO: KT-66071 update deserialization
    val gsonBuilder = GsonBuilder()
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
        }).registerTypeAdapter(File::class.java, object : JsonDeserializer<File> {
            override fun deserialize(
                json: JsonElement?,
                typeOfT: Type?,
                context: JsonDeserializationContext?,
            ): File? {
                val path: String? = context?.deserialize(/* json = */ json, /* typeOfT = */ String::class.java)
                return path?.let { File(it) }//ignore source changes right now
            }
        })
        // Add custom deserializer for BuildTimes class to handle dynamicBuildTimesNs properly
        .registerTypeAdapter(BuildTimes::class.java, object : JsonDeserializer<BuildTimes<GradleBuildTime>> {
            override fun deserialize(
                json: JsonElement?,
                typeOfT: Type?,
                context: JsonDeserializationContext?
            ): BuildTimes<GradleBuildTime>? {
                if (json == null || !json.isJsonObject) {
                    throw JsonParseException("Expected JsonObject for BuildTimes")
                }

                val jsonObject = json.asJsonObject
                val buildTimes = BuildTimes<GradleBuildTime>()

                // Handle buildTimesNs
                if (jsonObject.has("buildTimesNs") && jsonObject.get("buildTimesNs").isJsonObject) {
                    val buildTimesNsObj = jsonObject.getAsJsonObject("buildTimesNs")
                    for (entry in buildTimesNsObj.entrySet()) {
                        try {
                            val buildTime = GradleBuildTime.valueOf(entry.key)
                            val timeNs = entry.value.asLong
                            buildTimes.addTimeNs(buildTime, timeNs)
                        } catch (e: Exception) {
                            // Skip invalid entries
                        }
                    }
                }

                // Handle dynamicBuildTimesNs
                if (jsonObject.has("dynamicBuildTimesNs")) {
                    val dynamicElement = jsonObject.get("dynamicBuildTimesNs")
                    if (dynamicElement.isJsonObject) {
                        val dynamicObj = dynamicElement.asJsonObject
                        for (entry in dynamicObj.entrySet()) {
                            val keyStr = entry.key
                            val value = entry.value.asLong

                            // Parse DynamicBuildTimeKey from string representation
                            if (keyStr.startsWith("DynamicBuildTimeKey")) {
                                val nameMatch = "name=([^,)]+)".toRegex().find(keyStr)
                                val parentMatch = "parent=([^,)]+)".toRegex().find(keyStr)

                                if (nameMatch != null && parentMatch != null) {
                                    val name = nameMatch.groupValues[1]
                                    val parentStr = parentMatch.groupValues[1]
                                    val parent = try {
                                        GradleBuildTime.valueOf(parentStr)
                                    } catch (e: IllegalArgumentException) {
                                        // If we can't parse the parent, just use a placeholder
                                        GradleBuildTime.GRADLE_TASK
                                    }

                                    val key = DynamicBuildTimeKey(name, parent)
                                    buildTimes.addDynamicTimeNs(key, value)
                                }
                            }
                        }
                    }
                }

                return buildTimes
            }
        })

    val buildExecutionData = jsonReport.bufferedReader().use {
        gsonBuilder.create().fromJson(JsonReader(it), BuildExecutionData::class.java) as BuildExecutionData
    }
    return buildExecutionData
}
