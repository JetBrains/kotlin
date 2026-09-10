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
import kotlinx.serialization.serializerOrNull

/**
 * Pre-configured [Json] instances for use inside the Kotlin Gradle Plugin.
 *
 * Note: kotlinx-serialization is relocated to
 * `org.jetbrains.kotlin.gradle.internal.kotlinx.serialization` in the fat jar.
 * All KGP code should use these instances rather than creating its own [Json] objects,
 * so that serialization configuration is centralized.
 * The exception is a published format whose exact bytes older Kotlin versions depend on, such as
 * `kotlin-project-structure-metadata.json`: its instance sits next to its DTOs and sets every option itself,
 * so a change here can't alter it.
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

    /** As [default], but fails on a key the target class doesn't declare. For input whose every key must be understood. */
    val strict: Json = Json(default) {
        ignoreUnknownKeys = false
    }

    /**
     * As [default], but also accepts unquoted keys and strings, and comments. For files this plugin did not write;
     * Gson read them in its lenient mode, which took the same.
     */
    val lenient: Json = Json(default) {
        isLenient = true
        allowComments = true
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

/** kotlinx-serialization fails on a leading byte order mark. */
internal fun String.removeBom(): String = removePrefix("\uFEFF")

/**
 * Converts the values build authors pass to the JS DSLs as `Any`.
 *
 * Map entries with a null value are left out unless [serializeNulls] is set, as Gson did by default; webpack's schema
 * rejects an explicit null for many options. A `@Serializable` class is written by its serializer. Anything else fails:
 * Gson wrote such objects field by field, which hid typos and private state in the generated files.
 */
internal fun anyToJsonElement(value: Any?, serializeNulls: Boolean = false): JsonElement = when (value) {
    null -> JsonNull
    is JsonElement -> value
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Char -> JsonPrimitive(value.toString())
    is Map<*, *> -> buildJsonObject {
        value.forEach { (k, v) -> if (v != null || serializeNulls) put(k.toString(), anyToJsonElement(v, serializeNulls)) }
    }
    is Iterable<*> -> buildJsonArray {
        value.forEach { add(anyToJsonElement(it, serializeNulls)) }
    }
    is Array<*> -> anyToJsonElement(value.asList(), serializeNulls)
    is Enum<*> -> JsonPrimitive(value.name)
    is CharSequence -> JsonPrimitive(value.toString())
    else -> primitiveArrayToJsonElement(value, serializeNulls)
        ?: serializableToJsonElement(value)
        ?: error("Cannot write ${value.javaClass.name} to a JS configuration file. Pass a Map, a list or a primitive value instead.")
}

/** `null` unless [value] is an IntArray, BooleanArray or another primitive array, which are not `Array<*>`. */
private fun primitiveArrayToJsonElement(value: Any, serializeNulls: Boolean): JsonElement? {
    if (!value.javaClass.isArray) return null
    return buildJsonArray {
        for (i in 0 until java.lang.reflect.Array.getLength(value)) {
            add(anyToJsonElement(java.lang.reflect.Array.get(value, i), serializeNulls))
        }
    }
}

/** `null` when [value]'s class has no serializer, or one built against a different kotlinx-serialization copy. */
private fun serializableToJsonElement(value: Any): JsonElement? {
    val serializer = runCatching { serializerOrNull(value::class.java) }.getOrNull() ?: return null
    return KgpJson.default.encodeToJsonElement(serializer, value)
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
