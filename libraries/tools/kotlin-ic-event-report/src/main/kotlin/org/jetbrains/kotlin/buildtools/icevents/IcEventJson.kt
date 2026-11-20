/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.icevents

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent

private val IcJson = Json {
    ignoreUnknownKeys = true // to drop added unknown fields
    encodeDefaults = true    // since "type" is set with defaults
    prettyPrint = true
}

@Serializable
private class IcEventFile(val events: List<JsonObject>)

private fun IcEvent.toJsonOrNull(): JsonObject? = when (val e = this) {
    is IcEvent.CompilationStarted -> IcJson.encodeToJsonElement(
        CompilationStartedDto.serializer(),
        CompilationStartedDto(
            isIncremental = e.isIncremental,
            reason = e.reason,
            timestamp = e.timestamp,
            iteration = e.iteration,
        ),
    )
    is IcEvent.CompileIteration -> IcJson.encodeToJsonElement(
        CompileIterationDto.serializer(),
        CompileIterationDto(
            reasons = e.reasons.mapValues { (_, rs) -> rs.map { FileCompilationReasonDto(it.readableString) } },
            exitCode = e.exitCode,
            timestamp = e.timestamp,
            iteration = e.iteration,
        ),
    )
    else -> null
}?.jsonObject

/**
 * Encodes a list of IcEvent objects into a JSON string.
 *
 * @param events the list of IcEvent objects to encode.
 * @return a JSON string representing the list of IcEvent objects.
 */
@ExperimentalBuildToolsApi
public fun encodeIcEvents(events: List<IcEvent>): String =
    IcJson.encodeToString(IcEventFile.serializer(), IcEventFile(events = events.mapNotNull { it.toJsonOrNull() }))

/**
 * Decode a list of IcEvent objects from a JSON string.
 *
 * Handles version skew between the writer that created the JSON and the reader decoding it:
 * removing fields and subtypes of IcEvent and FileCompilationReason interfaces is not allowed,
 * and any additions there are safely ignored via the `ignoreUnknownKeys = true` option.
 *
 * Creates instances of the library's own DTO implementations of the IcEvent and FileCompilationReason interfaces.
 *
 * @param text the JSON string to decode.
 * @return a list of IcEvent objects.
 */
@ExperimentalBuildToolsApi
public fun decodeIcEvents(text: String): List<IcEvent> =
    IcJson.decodeFromString(IcEventFile.serializer(), text).events.mapNotNull { obj ->
        when ((obj["type"] as? JsonPrimitive)?.contentOrNull) {
            "CompilationStarted" -> IcJson.decodeFromJsonElement(CompilationStartedDto.serializer(), obj)
            "CompileIteration" -> IcJson.decodeFromJsonElement(CompileIterationDto.serializer(), obj)
            else -> null // unknown event subtypes are dropped
        }
    }
