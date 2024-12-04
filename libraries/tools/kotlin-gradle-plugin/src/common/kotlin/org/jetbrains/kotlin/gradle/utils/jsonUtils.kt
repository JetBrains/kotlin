/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import com.google.gson.*
import java.io.File
import java.lang.reflect.Type

internal object JsonUtils {
    internal val gson: Gson by lazy {
        GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setPrettyPrinting()
            .serializeNulls()
            .registerTypeHierarchyAdapter(File::class.java, FileAdapter)
            .create()
    }

    internal fun <K, V> toMap(jsonText: String): Map<K, V> {
        return gson.fromJson<Map<K, V>>(jsonText, Map::class.java)
    }


    private object FileAdapter : JsonSerializer<File>, JsonDeserializer<File> {
        override fun serialize(src: File, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.absolutePath)
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): File? {
            return json?.asString?.let { absolutePath -> File(absolutePath) }
        }
    }
}