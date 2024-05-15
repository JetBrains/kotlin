/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.Serializable
import java.lang.reflect.Type
import java.nio.file.Path
import java.nio.file.Paths

internal object SerializationTools {

    private val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeHierarchyAdapter(Path::class.java, PathTypeAdapter)
            .registerTypeHierarchyAdapter(File::class.java, FileTypeAdapter)
            .setPrettyPrinting()
            .create()
    }

    inline fun <reified T : List<Serializable>> writeToJson(objects: T, filePath: String) {
        val json = gson.toJson(objects)
        File(filePath).writeText(json)
    }

    inline fun <reified T : List<Serializable>> readFromJson(filePath: String): T {
        val jsonString = File(filePath).readText()
        val listType = object : TypeToken<T>() {}.type
        return gson.fromJson(jsonString, listType)
    }
}

private object PathTypeAdapter : JsonSerializer<Path>, JsonDeserializer<Path> {
    override fun serialize(src: Path?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Path {
        return Paths.get(json?.asString.orEmpty())
    }
}

private object FileTypeAdapter : JsonSerializer<File>, JsonDeserializer<File> {
    override fun serialize(src: File?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.canonicalPath)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): File {
        return File(json?.asString.orEmpty())
    }
}