/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask
import java.io.File

@Serializable
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
    fun read(): List<TransformedMetadataLibraryRecord> = json.decodeFromString(serializer, file.readText())

    fun write(records: List<TransformedMetadataLibraryRecord>) {
        file.writeText(json.encodeToString(serializer, records))
    }

    private companion object {
        val serializer = ListSerializer(TransformedMetadataLibraryRecord.serializer())

        /** Nulls are written, so every record shows the same keys, and the component id kind goes under "type". */
        val json = Json(KgpJson.prettyPrinted) {
            explicitNulls = true
            classDiscriminator = "type"
        }
    }
}

internal fun KotlinMetadataLibrariesIndexFile.readFiles() = read().map { File(it.file) }
