/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.reflect.TypeToken
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropMetadataDependencyTransformationTask
import java.io.File
import java.io.FileReader
import java.io.FileWriter

private val gson = GsonBuilder().setStrictness(Strictness.LENIENT).setPrettyPrinting().serializeNulls().create()

data class TransformedMetadataLibraryRecord(
    val moduleId: String,
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