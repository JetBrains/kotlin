/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.internal.compilerRunner.native

import kotlinx.serialization.json.*
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.util.*

/**
 * Serializes a [UnitStats] instance to a pretty-printed JSON string.
 *
 * The output format is compatible with the JSON consumed by [KotlinNativeToolRunner]'s
 * build-metrics parser and with what the Kotlin compiler itself emits.
 */
internal object UnitStatsJsonDumper {
    fun dump(stats: UnitStats): String =
        KgpJson.prettyPrinted.encodeToString(JsonElement.serializer(), stats.toJsonElement())

    private fun UnitStats.toJsonElement(): JsonElement = buildJsonObject {
        putNullable("name", name)
        putNullable("outputKind", outputKind)
        put("timeStampMs", timeStampMs)
        put("platform", platform.name)
        put("compilerType", compilerType.name)
        put("hasErrors", hasErrors)
        put("filesCount", filesCount)
        put("linesCount", linesCount)

        putNullableTime("initStats", initStats)
        putNullableTime("analysisStats", analysisStats)
        putNullableTime("translationToIrStats", translationToIrStats)
        putNullableTime("irPreLoweringStats", irPreLoweringStats)
        putNullableTime("irSerializationStats", irSerializationStats)
        putNullableTime("klibWritingStats", klibWritingStats)
        putNullableTime("irLinkingStats", irLinkingStats)
        putNullableTime("irLoweringStats", irLoweringStats)
        putNullableTime("backendStats", backendStats)

        val dyn = dynamicStats
        if (dyn != null) {
            put("dynamicStats", JsonArray(dyn.map { it.toJsonElement() }))
        }

        val klib = klibElementStats
        if (klib != null) {
            put("klibElementStats", JsonArray(klib.map { it.toJsonElement() }))
        }

        putNullableSideStats("findJavaClassStats", findJavaClassStats)
        putNullableSideStats("findKotlinClassStats", findKotlinClassStats)

        put("gcStats", JsonArray(gcStats.map { it.toJsonElement() }))
        putNullableLong("jitTimeMillis", jitTimeMillis)
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        if (value != null) put(key, value) else put(key, JsonNull)
    }

    private fun JsonObjectBuilder.putNullableLong(key: String, value: Long?) {
        if (value != null) put(key, value) else put(key, JsonNull)
    }

    private fun JsonObjectBuilder.putNullableTime(key: String, value: Time?) {
        if (value != null) put(key, value.toJsonElement()) else put(key, JsonNull)
    }

    private fun JsonObjectBuilder.putNullableSideStats(key: String, value: SideStats?) {
        if (value != null) put(key, value.toJsonElement()) else put(key, JsonNull)
    }

    private fun Time.toJsonElement(): JsonElement = buildJsonObject {
        put("nanos", nanos)
        put("userNanos", userNanos)
        put("cpuNanos", cpuNanos)
    }

    private fun SideStats.toJsonElement(): JsonElement = buildJsonObject {
        put("count", count)
        put("time", time.toJsonElement())
    }

    private fun GarbageCollectionStats.toJsonElement(): JsonElement = buildJsonObject {
        put("kind", kind)
        put("millis", millis)
        put("count", count)
    }

    private fun DynamicStats.toJsonElement(): JsonElement = buildJsonObject {
        put("parentPhaseType", parentPhaseType.name)
        put("name", name)
        put("time", time.toJsonElement())
    }

    private fun KlibElementStats.toJsonElement(): JsonElement = buildJsonObject {
        put("path", path)
        put("size", size)
    }
}
