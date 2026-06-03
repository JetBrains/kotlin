/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Utility that converts a loosely-typed `Any?` value representing a JSON-like
 * data structure into a `kotlinx.serialization` [JsonElement] (and from there
 * into a JSON string).
 *
 * The value is expected to form a tree built from the following types only:
 *  - `null`
 *  - [Boolean]
 *  - [Number] (e.g. [Int], [Long], [Double], [Float], [Short], [Byte])
 *  - [String]
 *  - [Map] with [String] keys (or keys whose `toString()` yields the JSON key);
 *    map keys must not be `null`.
 *  - [Iterable] (e.g. [List], [Set]) or arrays (`Array<*>`, primitive arrays).
 *
 * Any other type encountered while walking the tree causes an
 * [IllegalArgumentException] to be thrown, with a path describing where in the
 * tree the offending value was found (e.g. `root.foo[2].bar`).
 *
 * Note: this utility is intentionally decoupled from webpack or any other
 * specific consumer — it only knows how to serialize JSON-like Kotlin values.
 */
internal object AnyJsonSerializer {

    private val prettyJson = Json {
        prettyPrint = true
    }

    /**
     * Convert [value] to a [JsonElement].
     *
     * @throws IllegalArgumentException if [value] (or any nested element) is not
     * one of the supported JSON-like types listed in [AnyJsonSerializer].
     */
    fun toJsonElement(value: Any?): JsonElement = convert(value, "root")

    /**
     * Convert [value] to a pretty-printed JSON string.
     *
     * @throws IllegalArgumentException if [value] (or any nested element) is not
     * one of the supported JSON-like types listed in [AnyJsonSerializer].
     */
    fun toJsonString(value: Any?): String = prettyJson.encodeToString(JsonElement.serializer(), toJsonElement(value))

    private fun convert(value: Any?, path: String): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is Map<*, *> -> convertMap(value, path)
            is Iterable<*> -> convertIterable(value, path)
            is Array<*> -> convertIterable(value.asList(), path)
            is IntArray -> JsonArray(value.map { JsonPrimitive(it) })
            is LongArray -> JsonArray(value.map { JsonPrimitive(it) })
            is DoubleArray -> JsonArray(value.map { JsonPrimitive(it) })
            is FloatArray -> JsonArray(value.map { JsonPrimitive(it) })
            is ShortArray -> JsonArray(value.map { JsonPrimitive(it) })
            is ByteArray -> JsonArray(value.map { JsonPrimitive(it) })
            is BooleanArray -> JsonArray(value.map { JsonPrimitive(it) })
            is CharArray -> JsonArray(value.map { JsonPrimitive(it.toString()) })
            else -> throw IllegalArgumentException(
                "Unsupported value of type '${value::class.qualifiedName ?: value::class.simpleName}' at '$path'. " +
                        "Only null, Boolean, Number, String, Map and Iterable (or arrays) are supported."
            )
        }
    }

    private fun convertMap(map: Map<*, *>, path: String): JsonObject {
        val content = LinkedHashMap<String, JsonElement>(map.size)
        for ((rawKey, rawValue) in map) {
            if (rawKey == null) {
                throw IllegalArgumentException("Null key in map at '$path' is not allowed.")
            }
            val key = rawKey.toString()
            content[key] = convert(rawValue, "$path.$key")
        }
        return JsonObject(content)
    }

    private fun convertIterable(iterable: Iterable<*>, path: String): JsonArray {
        val list = ArrayList<JsonElement>()
        for ((index, element) in iterable.withIndex()) {
            list.add(convert(element, "$path[$index]"))
        }
        return JsonArray(list)
    }
}
