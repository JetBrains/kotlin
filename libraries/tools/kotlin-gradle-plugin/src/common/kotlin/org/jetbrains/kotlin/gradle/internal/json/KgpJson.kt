/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * Pre-configured [Json] instances for use inside the Kotlin Gradle Plugin.
 *
 * Note: kotlinx-serialization is relocated to
 * `org.jetbrains.kotlin.gradle.internal.kotlinx.serialization` in the fat jar.
 * All KGP code should use these instances rather than creating its own [Json] objects,
 * so that serialization configuration is centralized.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object KgpJson {
    /**
     * Default instance: lenient parser that ignores unknown JSON keys and coerces invalid
     * enum/primitive values to their defaults.
     * Produces compact (non-pretty) JSON output.
     *
     * Use for reading JSON from external/cached sources where forward-compatibility matters.
     */
    val default: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    /**
     * Pretty-printed instance for human-readable output (config files, diagnostics, etc.).
     * Inherits all leniency settings from [default].
     */
    val prettyPrinted: Json = Json(default) {
        prettyPrint = true
    }

    /**
     * As [prettyPrinted], but indented with two spaces to match Gson's pretty printer.
     *
     * Used for the files whose exact bytes are part of a contract: `package.json` is consumed by npm and yarn and
     * compared against the previous run, and the generated webpack and karma configs are executed as JavaScript.
     */
    @OptIn(ExperimentalSerializationApi::class)
    val prettyPrintedTwoSpaceIndent: Json = Json(prettyPrinted) {
        prettyPrintIndent = "  "
    }
}

/**
 * Recursively converts an arbitrary value to a [JsonElement].
 *
 * Intended for the loosely typed `Map`/`List`/`Any?` trees that the JS and webpack DSLs let build authors assemble,
 * where there is no schema to derive a serializer from.
 *
 * Note the difference from the reflective Gson serializers this replaced: Gson walked an unrecognised object's
 * fields, whereas anything that is not a primitive, map or collection here falls back to its `toString()`. Types
 * that must survive as JSON objects therefore need an explicit branch at the call site.
 */
internal fun anyToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Map<*, *> -> buildJsonObject {
        value.forEach { (k, v) -> put(k.toString(), anyToJsonElement(v)) }
    }
    is Iterable<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it)) }
    }
    is Array<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it)) }
    }
    else -> JsonPrimitive(value.toString())
}
