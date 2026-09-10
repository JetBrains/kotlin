/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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

    /** As [default], but also accepts unquoted keys and strings. For files this plugin did not write. */
    val lenient: Json = Json(default) {
        isLenient = true
    }

    /**
     * Pretty-printed instance for human-readable output (config files, diagnostics, etc.).
     * Inherits all leniency settings from [default].
     */
    val prettyPrinted: Json = Json(default) {
        prettyPrint = true
    }

    /** Two-space indent, as Gson wrote, so generated files don't change. */
    val prettyPrintedTwoSpaceIndent: Json = Json(prettyPrinted) {
        prettyPrintIndent = "  "
    }
}

/** Converts the `Map`/`List`/primitive trees that build authors pass to the JS DSLs. Other types are rejected. */
internal fun anyToJsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Char -> JsonPrimitive(value.toString())
    is Map<*, *> -> buildJsonObject {
        value.forEach { (k, v) -> put(k.toString(), anyToJsonElement(v)) }
    }
    is Iterable<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it)) }
    }
    is Array<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it)) }
    }
    is Enum<*> -> JsonPrimitive(value.name)
    is CharSequence -> JsonPrimitive(value.toString())
    else -> throw IllegalArgumentException("Cannot write ${value::class.java.name} as JSON, use a Map instead")
}

/** Writes an `Any` DSL property through [anyToJsonElement]. Write-only. */
internal object AnyAsJsonElementSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor get() = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Any) {
        encoder.encodeSerializableValue(JsonElement.serializer(), anyToJsonElement(value))
    }

    override fun deserialize(decoder: Decoder): Any =
        throw UnsupportedOperationException("AnyAsJsonElementSerializer is write-only")
}
