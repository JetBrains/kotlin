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
 * Code dealing with JS/npm tooling should use these instances rather than creating its own [Json] objects,
 * so that serialization configuration stays centralized.
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
     * As [default], but also accepts unquoted keys and single-quoted strings, like Gson's reader did.
     *
     * For JSON this plugin did not write, such as a `package.json` coming from a dependency.
     */
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

    /**
     * As [prettyPrinted], but indented with two spaces to match Gson's pretty printer.
     *
     * Used where the exact bytes matter: npm and yarn read `package.json`, and the generated webpack and karma
     * configs are executed as JavaScript.
     */
    @OptIn(ExperimentalSerializationApi::class)
    val prettyPrintedTwoSpaceIndent: Json = Json(prettyPrinted) {
        prettyPrintIndent = "  "
    }
}

/**
 * Recursively converts an arbitrary value to a [JsonElement].
 *
 * For the `Map`/`List`/`Any?` trees the JS and webpack DSLs let build authors assemble, where there is no schema
 * to derive a serializer from.
 *
 * Gson reflected over the fields of an object it did not recognise. Here such a value is rejected instead of being
 * mangled into its `toString()`: types that have to survive as JSON objects need an explicit branch at the call
 * site, and build authors pass a [Map].
 *
 * @throws IllegalArgumentException if [value] has no JSON representation.
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
    // Gson wrote the constant name
    is Enum<*> -> JsonPrimitive(value.name)
    is CharSequence -> JsonPrimitive(value.toString())
    else -> throw IllegalArgumentException(
        "Cannot convert a value of type ${value::class.java.name} to JSON: only null, primitives, enums, maps, " +
                "collections and arrays are supported. Pass a Map to get a JSON object, or build a JsonElement " +
                "explicitly."
    )
}

/**
 * Serializer for the `Any` properties the JS DSLs expose, converting through [anyToJsonElement].
 *
 * Write-only: the original type cannot be recovered from JSON, and nothing in the plugin reads these files back.
 */
internal object AnyAsJsonElementSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor get() = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Any) {
        encoder.encodeSerializableValue(JsonElement.serializer(), anyToJsonElement(value))
    }

    override fun deserialize(decoder: Decoder): Any =
        throw UnsupportedOperationException("${'$'}{descriptor.serialName} is write-only")
}
