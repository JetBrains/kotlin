/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.internal.json.KgpJson

/**
 * DTOs mirroring the on-disk shape of `kotlin-project-structure-metadata.json`, kept separate from
 * [KotlinProjectStructureMetadata] so that the internal model can evolve without changing the wire format.
 *
 * The format is published inside metadata jars as `META-INF/kotlin-project-structure-metadata.json` and is read
 * back during dependency resolution and IDE import, so it must stay compatible with what previous Kotlin versions
 * wrote. Two legacy quirks are therefore reproduced verbatim:
 *  - booleans ([ProjectStructureNodeJson.isPublishedAsRoot], [SourceSetNodeJson.hostSpecific]) are quoted strings;
 *  - [SourceSetNodeJson.hostSpecific] is omitted rather than set to `"false"`.
 *
 * Reading stays as permissive as Gson was, because other producers and hand-patched files rely on it: unknown
 * keys are ignored, a missing `isPublishedAsRoot` defaults to `"false"`, and the quoted booleans also accept the
 * unquoted form (see [QuotedBooleanAsStringSerializer]). `JsonParser.parseReader` always parsed leniently, so
 * `isLenient` is on and [decodeKotlinProjectStructureMetadataJson] drops the BOM its reader skipped. Gson went a
 * little further and took single-quoted strings, which `isLenient` reads as literals with the quotes kept, but
 * nothing is known to write those.
 *
 * Property declaration order defines the order of keys in the output and must match
 * [org.jetbrains.kotlin.gradle.plugin.mpp.serialize], which the XML representation still uses.
 */
@Serializable
internal data class KotlinProjectStructureMetadataJson(
    val projectStructure: ProjectStructureNodeJson,
)

@Serializable
internal data class ProjectStructureNodeJson(
    val formatVersion: String,
    @Serializable(with = QuotedBooleanAsStringSerializer::class)
    val isPublishedAsRoot: String = "false",
    val variants: List<VariantNodeJson> = emptyList(),
    val sourceSets: List<SourceSetNodeJson> = emptyList(),
)

@Serializable
internal data class VariantNodeJson(
    val name: String,
    val sourceSet: List<String> = emptyList(),
)

@Serializable
internal data class SourceSetNodeJson(
    val name: String,
    val dependsOn: List<String> = emptyList(),
    /** `"$groupId:$moduleId"` pairs, see [ModuleDependencyIdentifier]. */
    val moduleDependency: List<String> = emptyList(),
    val sourceSetCInteropMetadataDirectory: String? = null,
    val binaryLayout: String? = null,
    @Serializable(with = QuotedBooleanAsStringSerializer::class)
    val hostSpecific: String? = null,
)

/**
 * Keeps reading of the quoted booleans as lenient as Gson's `asString` was: it accepted both `"true"` and a bare
 * `true`, while kotlinx-serialization would reject the latter with a type mismatch (`coerceInputValues` does not
 * help — it only substitutes defaults for an explicit `null`). Decoding goes through [kotlinx.serialization.json.JsonPrimitive.content],
 * which is indifferent to quoting; encoding always emits the quoted form the on-disk format has always used.
 */
internal object QuotedBooleanAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("QuotedBoolean", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String =
        if (decoder is JsonDecoder) decoder.decodeJsonElement().jsonPrimitive.content else decoder.decodeString()
}

/**
 * Gson's pretty printer, which used to produce this file, indents with two spaces, while kotlinx-serialization
 * defaults to four. The indentation is part of the contract: `libraries/stdlib` and `libraries/kotlin.test` compare
 * the generated file against checked-in expectations with exact string equality.
 *
 * One difference is patched up in [encodeToString]: empty arrays — and `encodeDefaults = true` means
 * empty `dependsOn` / `moduleDependency` / `variants` / `sourceSets` are always written — come out as
 *
 *     "dependsOn": [
 *     ],
 *
 * rather than Gson's `"dependsOn": []`. That is a pretty-printer bug in kotlinx-serialization-json 1.4.1, the
 * version embedded into KGP for the Gradle 7.6 variant (`GradlePluginVariant.GRADLE_MIN`, pinned there because
 * 1.4.1 is the last release built against the Kotlin 1.7.10 stdlib that Gradle 7.6 ships). 1.5.0 fixed it by
 * calling `Composer.nextItemIfNotFirst()` instead of `nextItem()` in `StreamingJsonEncoder.endStructure`.
 * They are rewritten here rather than left to the runtime, because the shipped plugin and the functionalTest
 * classpath do not resolve the same version.
 *
 * Gson's HTML escaping is not reproduced: `GsonBuilder` writes `<`, `>`, `&`, `=` and `'` as `\uXXXX` by
 * default. Those do not occur in variant or source set names, so the bytes only differ if one ever shows up.
 */
@OptIn(ExperimentalSerializationApi::class)
private val projectStructureMetadataJson = Json(KgpJson.prettyPrinted) {
    prettyPrintIndent = "  "
    isLenient = true
}

/** A JSON string token cannot contain a raw newline, so this matches nothing but an empty array. */
private val PRETTY_PRINTED_EMPTY_ARRAY = """\[\s*\n\s*]""".toRegex()

/** Gson's reader skipped the UTF-8 byte order mark; kotlinx-serialization chokes on it. */
private const val BOM = "\uFEFF"

internal fun KotlinProjectStructureMetadataJson.encodeToString(): String =
    projectStructureMetadataJson.encodeToString(KotlinProjectStructureMetadataJson.serializer(), this)
        .replace(PRETTY_PRINTED_EMPTY_ARRAY, "[]")

internal fun decodeKotlinProjectStructureMetadataJson(string: String): KotlinProjectStructureMetadataJson =
    projectStructureMetadataJson.decodeFromString(KotlinProjectStructureMetadataJson.serializer(), string.removePrefix(BOM))
