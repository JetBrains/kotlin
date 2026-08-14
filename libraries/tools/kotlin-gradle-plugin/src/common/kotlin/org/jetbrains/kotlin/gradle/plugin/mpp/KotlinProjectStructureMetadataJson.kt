/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.gradle.internal.json.KgpJson

/**
 * JSON shape of `META-INF/kotlin-project-structure-metadata.json`.
 *
 * Older Kotlin versions read and write this file, so the shape can't change: booleans are strings,
 * `hostSpecific` is written only when true, and keys follow property order. Reading is lenient because
 * some files come from other tools.
 */
@Serializable
internal data class KotlinProjectStructureMetadataJson(
    val projectStructure: ProjectStructureNodeJson,
)

@Serializable
internal data class ProjectStructureNodeJson(
    val formatVersion: String,
    @Serializable(with = QuotedBooleanSerializer::class)
    val isPublishedAsRoot: Boolean = false,
    val variants: List<VariantNodeJson> = emptyList(),
    val sourceSets: List<SourceSetNodeJson> = emptyList(),
)

@Serializable
internal data class VariantNodeJson(
    val name: String,
    val sourceSet: List<String> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SourceSetNodeJson(
    val name: String,
    val dependsOn: List<String> = emptyList(),
    val moduleDependency: List<@Serializable(with = ModuleDependencyIdentifierSerializer::class) ModuleDependencyIdentifier> = emptyList(),
    val sourceSetCInteropMetadataDirectory: String? = null,
    val binaryLayout: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = QuotedBooleanSerializer::class)
    val hostSpecific: Boolean = false,
)

/** Written as `"true"`/`"false"`. The lenient reader also takes bare `true`/`false`. */
internal object QuotedBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("QuotedBoolean", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeString().toBoolean()
}

/** Written as `groupId:moduleId`. */
internal object ModuleDependencyIdentifierSerializer : KSerializer<ModuleDependencyIdentifier> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ModuleDependencyIdentifier", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ModuleDependencyIdentifier) =
        encoder.encodeString("${value.groupId}:${value.moduleId}")

    override fun deserialize(decoder: Decoder): ModuleDependencyIdentifier = parseModuleDependencyIdentifier(decoder.decodeString())
}

// The settings that shape the file are set here, not inherited from KgpJson.
// stdlib and kotlin.test compare the output with checked-in files, hence the two-space indent.
@OptIn(ExperimentalSerializationApi::class)
private val projectStructureMetadataJson = Json(KgpJson.prettyPrinted) {
    prettyPrintIndent = "  "
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
    isLenient = true
    allowComments = true
}

/** kotlinx-serialization fails on a leading byte order mark. */
private const val BOM = "\uFEFF"

internal fun KotlinProjectStructureMetadataJson.encodeToString(): String =
    projectStructureMetadataJson.encodeToString(KotlinProjectStructureMetadataJson.serializer(), this)

internal fun decodeKotlinProjectStructureMetadataJson(string: String): KotlinProjectStructureMetadataJson =
    projectStructureMetadataJson.decodeFromString(KotlinProjectStructureMetadataJson.serializer(), string.removePrefix(BOM))
