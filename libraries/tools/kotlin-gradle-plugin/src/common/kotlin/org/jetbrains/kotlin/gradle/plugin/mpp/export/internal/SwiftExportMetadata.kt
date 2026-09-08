/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.export.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.export.tasks.SerializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.createConsumable
import java.io.InputStream
import java.io.OutputStream
import java.io.Serializable

private const val SWIFT_EXPORT_METADATA_USAGE = "swiftExportMetadata"

/**
 * Version of the [SwiftExportMetadata] serialization format. Bump it whenever the serialized shape changes.
 */
internal const val SWIFT_EXPORT_METADATA_SCHEMA_VERSION = 1

/**
 * How a library configured with the `export { swift { } }` DSL exposes itself to Swift. Only the module-level part
 * of the DSL is published; the rest of it (e.g. Xcode integration settings) is about the producer's own build.
 */
@kotlinx.serialization.Serializable
internal data class SwiftExportMetadata(
    val schemaVersion: Int = SWIFT_EXPORT_METADATA_SCHEMA_VERSION,
    val moduleName: String?,
    val rootPackage: String?,
) : Serializable

internal fun deserializeSwiftExportMetadata(inputStream: InputStream) =
    KgpJson.default.decodeFromStream<SwiftExportMetadata>(inputStream)

internal fun SwiftExportMetadata.serializeSwiftExportMetadata(outputStream: OutputStream) =
    KgpJson.default.encodeToStream(this, outputStream)

/**
 * Consumable configuration that carries the Swift Export metadata artifact.
 */
internal fun Project.registerSwiftExportMetadataApiElements(
    swiftExportMetadata: TaskProvider<SerializeSwiftExportMetadata>,
): Configuration {
    return project.configurations.createConsumable("swiftExportMetadataElements") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFT_EXPORT_METADATA_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        outgoing.artifact(swiftExportMetadata) {
            it.classifier = "swift-export-metadata"
            it.extension = "json"
        }
    }.get()
}
