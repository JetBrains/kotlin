/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type

private val gson = GsonBuilder()
    .setStrictness(Strictness.LENIENT)
    .setPrettyPrinting()
    .serializeNulls()
    .registerTypeHierarchyAdapter(KmpModuleIdentifier.GradleComponentId::class.java, KmpModuleIdentifierComponentIdAdapter)
    .create()

internal object KmpModuleIdentifierComponentIdAdapter : JsonSerializer<KmpModuleIdentifier.GradleComponentId>, JsonDeserializer<KmpModuleIdentifier.GradleComponentId> {
    override fun serialize(src: KmpModuleIdentifier.GradleComponentId, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().also {
            when (src) {
                is KmpModuleIdentifier.ModuleComponentId -> {
                    it.addProperty("type", "module")
                    it.addProperty("group", src.group)
                    it.addProperty("name", src.name)
                }
                is KmpModuleIdentifier.ProjectComponentId -> {
                    it.addProperty("type", "project")
                    it.addProperty("projectPath", src.projectPath)
                    it.addProperty("buildPath", src.buildPath)
                }
            }
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): KmpModuleIdentifier.GradleComponentId {
        require(json.isJsonObject) { "Expected JsonObject, got ${json.javaClass}" }
        require(json.asJsonObject.has("type")) { "JsonObject must contain 'type' property" }
        json as JsonObject
        val type = json.get("type").asString

        return when (type) {
            "module" -> {
                KmpModuleIdentifier.ModuleComponentId(
                    group = json.get("group").asString,
                    name = json.get("name").asString
                )
            }
            "project" -> {
                KmpModuleIdentifier.ProjectComponentId(
                    projectPath = json.get("projectPath").asString,
                    buildPath = json.get("buildPath").asString
                )
            }
            else -> error("Unknown ComponentId type: $type")
        }
    }
}

internal data class TransformedMetadataLibraryRecord(
    val moduleId: KmpModuleIdentifier,
    val file: String,
    val sourceSetName: String? = null
)

/**
 * Files used by the [MetadataDependencyTransformationTask] and [CInteropMetadataDependencyTransformationTask] to
 * store the resulting 'metadata path' in this index file.
 */
internal class KotlinMetadataLibrariesIndexFile(private val file: File) {
    private val typeToken = object : TypeToken<Collection<TransformedMetadataLibraryRecord>>() {}

    fun read(): List<TransformedMetadataLibraryRecord> = FileReader(file).use {
        gson.fromJson<Collection<TransformedMetadataLibraryRecord>>(it, typeToken.type).toList()
    }

    fun write(records: List<TransformedMetadataLibraryRecord>) {
        FileWriter(file).use {
            gson.toJson(records, typeToken.type, it)
        }
    }
}

internal fun KotlinMetadataLibrariesIndexFile.readFiles() = read().map { File(it.file) }
