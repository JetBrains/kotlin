/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    val isPublishedAsRoot: String,
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
    val hostSpecific: String? = null,
)

/**
 * Gson's pretty printer, which used to produce this file, indents with two spaces, while kotlinx-serialization
 * defaults to four. The indentation is part of the contract: `libraries/stdlib` and `libraries/kotlin.test` compare
 * the generated file against checked-in expectations with exact string equality.
 */
@OptIn(ExperimentalSerializationApi::class)
private val projectStructureMetadataJson = Json(KgpJson.prettyPrinted) {
    prettyPrintIndent = "  "
}

internal fun KotlinProjectStructureMetadataJson.encodeToString(): String =
    projectStructureMetadataJson.encodeToString(KotlinProjectStructureMetadataJson.serializer(), this)

internal fun decodeKotlinProjectStructureMetadataJson(string: String): KotlinProjectStructureMetadataJson =
    projectStructureMetadataJson.decodeFromString(KotlinProjectStructureMetadataJson.serializer(), string)
