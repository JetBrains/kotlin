/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModuleType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModules
import java.io.File

/**
 * Reads and writes the Swift Export modules file. Paths in the file are relative to the directory the file is in,
 * which lets the output of a run be moved around. [GradleSwiftExportModules] still gets absolute files.
 */
internal object SerializationTools {

    /** Writes [modules] with their paths relative to [baseDirectory]. Every file has to be inside it. */
    fun writeToJson(modules: GradleSwiftExportModules, baseDirectory: File): String =
        KgpJson.prettyPrinted.encodeToString(GradleSwiftExportModulesSerializer(RelativePaths(baseDirectory)), modules)

    /** Reads modules written by [writeToJson], resolving their paths against [baseDirectory]. */
    fun readFromJson(json: String, baseDirectory: File): GradleSwiftExportModules =
        KgpJson.prettyPrinted.decodeFromString(GradleSwiftExportModulesSerializer(RelativePaths(baseDirectory)), json)
}

/** Converts the paths of the modules file to and from paths relative to [baseDirectory], with `/` separators. */
private class RelativePaths(baseDirectory: File) {
    private val baseDirectory = baseDirectory.absoluteFile.normalize()

    fun relativize(file: File): String {
        val relative = file.absoluteFile.normalize().relativeToOrNull(baseDirectory)?.invariantSeparatorsPath
        require(relative != null && relative != ".." && !relative.startsWith("../")) {
            "Swift Export file $file is outside of $baseDirectory, so the modules file can't refer to it"
        }
        return relative
    }

    fun resolve(path: String): File = baseDirectory.resolve(path)
}

private class GradleSwiftExportFilesSerializer(private val paths: RelativePaths) : KSerializer<GradleSwiftExportFiles> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GradleSwiftExportFiles") {
        element<String>("swiftApi")
        element<String>("kotlinBridges")
        element<String>("cHeaderBridges")
    }

    override fun serialize(encoder: Encoder, value: GradleSwiftExportFiles) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, paths.relativize(value.swiftApi))
            encodeStringElement(descriptor, 1, paths.relativize(value.kotlinBridges))
            encodeStringElement(descriptor, 2, paths.relativize(value.cHeaderBridges))
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
            GradleSwiftExportFiles(paths.resolve(swiftApi), paths.resolve(kotlinBridges), paths.resolve(cHeaderBridges))
        }
    }
}

private class GradleSwiftExportModuleSerializer(private val paths: RelativePaths) : KSerializer<GradleSwiftExportModule> {
    private val stringListSerializer = ListSerializer(String.serializer())
    private val filesSerializer = GradleSwiftExportFilesSerializer(paths)

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
                composite.encodeSerializableElement(descriptor, 0, filesSerializer, value.files)
                composite.encodeStringElement(descriptor, 1, value.bridgeName)
            }
            is GradleSwiftExportModule.SwiftOnly -> {
                composite.encodeStringElement(descriptor, 2, paths.relativize(value.swiftApi))
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
                    0 -> files = decodeSerializableElement(descriptor, 0, filesSerializer)
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
                    paths.resolve(swiftApi ?: error("swiftApi is required for SWIFT_ONLY")),
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

private class GradleSwiftExportModulesSerializer(paths: RelativePaths) : KSerializer<GradleSwiftExportModules> {
    private val moduleListSerializer = ListSerializer(GradleSwiftExportModuleSerializer(paths))

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GradleSwiftExportModules") {
        element<String>("modules")
    }

    override fun serialize(encoder: Encoder, value: GradleSwiftExportModules) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, moduleListSerializer, value.modules)
        }
    }

    override fun deserialize(decoder: Decoder): GradleSwiftExportModules {
        return decoder.decodeStructure(descriptor) {
            var modules: List<GradleSwiftExportModule> = emptyList()
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> modules = decodeSerializableElement(descriptor, 0, moduleListSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            GradleSwiftExportModules(modules)
        }
    }
}
