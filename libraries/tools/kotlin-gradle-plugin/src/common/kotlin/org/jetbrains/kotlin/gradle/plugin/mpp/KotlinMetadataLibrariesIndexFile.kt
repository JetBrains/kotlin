/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask
import java.io.File

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
    @OptIn(ExperimentalSerializationApi::class)
    fun read(): List<TransformedMetadataLibraryRecord> {
        val json = KgpJson.default.decodeFromString(
            ListSerializer(TransformedMetadataLibraryRecordJson.serializer()),
            file.readText()
        )
        return json.map { it.toRecord() }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun write(records: List<TransformedMetadataLibraryRecord>) {
        val json = records.map { it.toJson() }
        file.writeText(
            KgpJson.prettyPrinted.encodeToString(
                ListSerializer(TransformedMetadataLibraryRecordJson.serializer()),
                json
            )
        )
    }
}

internal fun KotlinMetadataLibrariesIndexFile.readFiles() = read().map { File(it.file) }
