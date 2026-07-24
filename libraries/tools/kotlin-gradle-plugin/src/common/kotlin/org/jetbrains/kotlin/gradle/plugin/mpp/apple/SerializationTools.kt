/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModuleType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModules
import java.io.File

internal object SerializationTools {

    private val json: Json by lazy {
        Json(KgpJson.prettyPrinted) {
            serializersModule = SerializersModule {
                contextual(FileSerializer)
                contextual(GradleSwiftExportModuleSerializer)
            }
        }
    }

    fun writeToJson(objects: GradleSwiftExportModules): String =
        json.encodeToString(GradleSwiftExportModulesSerializer, objects)

    fun readFromJson(jsonString: String): GradleSwiftExportModules =
        json.decodeFromString(GradleSwiftExportModulesSerializer, jsonString)
}

internal object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("File", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): File {
        return File(decoder.decodeString())
    }
}

internal object GradleSwiftExportFilesSerializer : KSerializer<GradleSwiftExportFiles> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GradleSwiftExportFiles") {
        element<String>("swiftApi")
        element<String>("kotlinBridges")
        element<String>("cHeaderBridges")
    }

    override fun serialize(encoder: Encoder, value: GradleSwiftExportFiles) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.swiftApi.path)
            encodeStringElement(descriptor, 1, value.kotlinBridges.path)
            encodeStringElement(descriptor, 2, value.cHeaderBridges.path)
        }
    }

    override fun deserialize(decoder: Decoder): GradleSwiftExportFiles {
        return decoder.decodeStructure(descriptor) {
            var swiftApi = ""
            var kotlinBridges = ""
            var cHeaderBridges = ""
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> swiftApi = decodeStringElement(descriptor, 0)
                    1 -> kotlinBridges = decodeStringElement(descriptor, 1)
                    2 -> cHeaderBridges = decodeStringElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            GradleSwiftExportFiles(File(swiftApi), File(kotlinBridges), File(cHeaderBridges))
        }
    }
}

internal object GradleSwiftExportModuleSerializer : KSerializer<GradleSwiftExportModule> {
    private val stringListSerializer = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GradleSwiftExportModule") {
        element<String>("files", isOptional = true)
        element<String>("bridgeName", isOptional = true)
        element<String>("swiftApi", isOptional = true)
        element<String>("name")
        element<String>("type")
        element<String>("dependencies")
    }

    override fun serialize(encoder: Encoder, value: GradleSwiftExportModule) {
        val composite = encoder.beginStructure(descriptor)
        when (value) {
            is GradleSwiftExportModule.BridgesToKotlin -> {
                composite.encodeSerializableElement(descriptor, 0, GradleSwiftExportFilesSerializer, value.files)
                composite.encodeStringElement(descriptor, 1, value.bridgeName)
            }
            is GradleSwiftExportModule.SwiftOnly -> {
                composite.encodeStringElement(descriptor, 2, value.swiftApi.path)
            }
        }
        composite.encodeStringElement(descriptor, 3, value.name)
        composite.encodeStringElement(descriptor, 4, value.type.name)
        composite.encodeSerializableElement(descriptor, 5, stringListSerializer, value.dependencies)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): GradleSwiftExportModule {
        return decoder.decodeStructure(descriptor) {
            var files: GradleSwiftExportFiles? = null
            var bridgeName: String? = null
            var swiftApi: String? = null
            var name = ""
            var type = ""
            var dependencies: List<String> = emptyList()
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> files = decodeSerializableElement(descriptor, 0, GradleSwiftExportFilesSerializer)
                    1 -> bridgeName = decodeStringElement(descriptor, 1)
                    2 -> swiftApi = decodeStringElement(descriptor, 2)
                    3 -> name = decodeStringElement(descriptor, 3)
                    4 -> type = decodeStringElement(descriptor, 4)
                    5 -> dependencies = decodeSerializableElement(descriptor, 5, stringListSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            when (GradleSwiftExportModuleType.valueOf(type)) {
                GradleSwiftExportModuleType.SWIFT_ONLY -> GradleSwiftExportModule.SwiftOnly(
                    File(swiftApi ?: error("swiftApi is required for SWIFT_ONLY")),
                    name,
                    dependencies
                )
                GradleSwiftExportModuleType.BRIDGES_TO_KOTLIN -> GradleSwiftExportModule.BridgesToKotlin(
                    files ?: error("files is required for BRIDGES_TO_KOTLIN"),
                    bridgeName ?: error("bridgeName is required for BRIDGES_TO_KOTLIN"),
                    name,
                    dependencies
                )
            }
        }
    }
}

internal object GradleSwiftExportModulesSerializer : KSerializer<GradleSwiftExportModules> {
    private val moduleListSerializer = ListSerializer(GradleSwiftExportModuleSerializer)

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GradleSwiftExportModules") {
        element<String>("modules")
        element<Long>("timestamp")
    }

    override fun serialize(encoder: Encoder, value: GradleSwiftExportModules) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, moduleListSerializer, value.modules)
            encodeLongElement(descriptor, 1, value.timestamp)
        }
    }

    override fun deserialize(decoder: Decoder): GradleSwiftExportModules {
        return decoder.decodeStructure(descriptor) {
            var modules: List<GradleSwiftExportModule> = emptyList()
            var timestamp = 0L
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> modules = decodeSerializableElement(descriptor, 0, moduleListSerializer)
                    1 -> timestamp = decodeLongElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            GradleSwiftExportModules(modules, timestamp)
        }
    }
}
