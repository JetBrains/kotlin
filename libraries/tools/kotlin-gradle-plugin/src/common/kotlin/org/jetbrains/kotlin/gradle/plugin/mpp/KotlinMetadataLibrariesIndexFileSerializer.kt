/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Serializable
internal data class TransformedMetadataLibraryRecordJson(
    val moduleId: KmpModuleIdentifierJson,
    val file: String,
    val sourceSetName: String? = null,
)

@Serializable
internal data class KmpModuleIdentifierJson(
    val groupAndName: GradleGroupAndNameJson? = null,
    @Serializable(with = GradleComponentIdJsonSerializer::class)
    val componentId: GradleComponentIdJson,
)

@Serializable
internal data class GradleGroupAndNameJson(
    val group: String,
    val name: String,
)

/**
 * Flat polymorphic JSON representation of [KmpModuleIdentifier.GradleComponentId].
 * Matches the legacy Gson format with a `"type"` discriminator field.
 */
internal sealed class GradleComponentIdJson {
    abstract val type: String

    data class ModuleComponentIdJson(
        override val type: String = "module",
        val group: String,
        val name: String,
    ) : GradleComponentIdJson()

    data class ProjectComponentIdJson(
        override val type: String = "project",
        val projectPath: String,
        val buildPath: String,
    ) : GradleComponentIdJson()
}

internal object GradleComponentIdJsonSerializer : KSerializer<GradleComponentIdJson> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GradleComponentIdJson") {
        element<String>("type")
        element<String>("group", isOptional = true)
        element<String>("name", isOptional = true)
        element<String>("projectPath", isOptional = true)
        element<String>("buildPath", isOptional = true)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: GradleComponentIdJson) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            val obj = buildJsonObject {
                put("type", value.type)
                when (value) {
                    is GradleComponentIdJson.ModuleComponentIdJson -> {
                        put("group", value.group)
                        put("name", value.name)
                    }
                    is GradleComponentIdJson.ProjectComponentIdJson -> {
                        put("projectPath", value.projectPath)
                        put("buildPath", value.buildPath)
                    }
                }
            }
            jsonEncoder.encodeJsonElement(obj)
        } else {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.type)
                when (value) {
                    is GradleComponentIdJson.ModuleComponentIdJson -> {
                        encodeStringElement(descriptor, 1, value.group)
                        encodeStringElement(descriptor, 2, value.name)
                    }
                    is GradleComponentIdJson.ProjectComponentIdJson -> {
                        encodeStringElement(descriptor, 3, value.projectPath)
                        encodeStringElement(descriptor, 4, value.buildPath)
                    }
                }
            }
        }
    }

    override fun deserialize(decoder: Decoder): GradleComponentIdJson {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val obj = jsonDecoder.decodeJsonElement().jsonObject
            val type = obj.getValue("type").let {
                jsonDecoder.json.decodeFromJsonElement<String>(it)
            }
            return when (type) {
                "module" -> GradleComponentIdJson.ModuleComponentIdJson(
                    group = obj.getValue("group").let { jsonDecoder.json.decodeFromJsonElement<String>(it) },
                    name = obj.getValue("name").let { jsonDecoder.json.decodeFromJsonElement<String>(it) },
                )
                "project" -> GradleComponentIdJson.ProjectComponentIdJson(
                    projectPath = obj.getValue("projectPath").let { jsonDecoder.json.decodeFromJsonElement<String>(it) },
                    buildPath = obj.getValue("buildPath").let { jsonDecoder.json.decodeFromJsonElement<String>(it) },
                )
                else -> error("Unknown ComponentId type: $type")
            }
        } else {
            var type = ""
            var group: String? = null
            var name: String? = null
            var projectPath: String? = null
            var buildPath: String? = null
            decoder.decodeStructure(descriptor) {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> type = decodeStringElement(descriptor, 0)
                        1 -> group = decodeStringElement(descriptor, 1)
                        2 -> name = decodeStringElement(descriptor, 2)
                        3 -> projectPath = decodeStringElement(descriptor, 3)
                        4 -> buildPath = decodeStringElement(descriptor, 4)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index $index")
                    }
                }
            }
            return when (type) {
                "module" -> GradleComponentIdJson.ModuleComponentIdJson(
                    group = group ?: error("Missing 'group'"),
                    name = name ?: error("Missing 'name'"),
                )
                "project" -> GradleComponentIdJson.ProjectComponentIdJson(
                    projectPath = projectPath ?: error("Missing 'projectPath'"),
                    buildPath = buildPath ?: error("Missing 'buildPath'"),
                )
                else -> error("Unknown ComponentId type: $type")
            }
        }
    }
}

internal fun TransformedMetadataLibraryRecord.toJson(): TransformedMetadataLibraryRecordJson =
    TransformedMetadataLibraryRecordJson(
        moduleId = KmpModuleIdentifierJson(
            groupAndName = moduleId.groupAndName?.let { GradleGroupAndNameJson(it.group, it.name) },
            componentId = when (val id = moduleId.componentId) {
                is KmpModuleIdentifier.ModuleComponentId -> GradleComponentIdJson.ModuleComponentIdJson(
                    group = id.group,
                    name = id.name,
                )
                is KmpModuleIdentifier.ProjectComponentId -> GradleComponentIdJson.ProjectComponentIdJson(
                    projectPath = id.projectPath,
                    buildPath = id.buildPath,
                )
            }
        ),
        file = file,
        sourceSetName = sourceSetName,
    )

internal fun TransformedMetadataLibraryRecordJson.toRecord(): TransformedMetadataLibraryRecord =
    TransformedMetadataLibraryRecord(
        moduleId = KmpModuleIdentifier(
            groupAndName = moduleId.groupAndName?.let { KmpModuleIdentifier.GradleGroupAndName(it.group, it.name) },
            componentId = when (val id = moduleId.componentId) {
                is GradleComponentIdJson.ModuleComponentIdJson -> KmpModuleIdentifier.ModuleComponentId(
                    group = id.group,
                    name = id.name,
                )
                is GradleComponentIdJson.ProjectComponentIdJson -> KmpModuleIdentifier.ProjectComponentId(
                    projectPath = id.projectPath,
                    buildPath = id.buildPath,
                )
            }
        ),
        file = file,
        sourceSetName = sourceSetName,
    )
