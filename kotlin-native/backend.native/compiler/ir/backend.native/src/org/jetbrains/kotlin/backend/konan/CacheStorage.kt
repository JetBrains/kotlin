/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.EagerInitializedPropertySerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer

internal class CacheStorage(private val outputFiles: CacheOutputs) {

    companion object {
        fun renameOutput(outputFiles: CacheOutputs) {
            // For caches the output file is a directory. It might be created by someone else,
            // we have to delete it in order for the next renaming operation to succeed.
            // TODO: what if the directory is not empty?
            java.io.File(outputFiles.mainFileName).delete()
            if (!outputFiles.tempCacheDirectory.renameTo(outputFiles.mainFile))
                outputFiles.tempCacheDirectory.deleteRecursively()
        }
    }

    fun saveAdditionalCacheInfo(cacheAdditionalInfo: CacheAdditionalInfo?, dependenciesTrackingResult: DependenciesTrackingResult) {
        outputFiles.prepareTempDirectories()
        cacheAdditionalInfo?.let {
            saveInlineFunctionBodies(it.inlineFunctionBodies)
            saveClassFields(it.classFields)
            saveEagerInitializedProperties(it.eagerInitializedFiles)
        }
        saveCacheBitcodeDependencies(dependenciesTrackingResult.immediateBitcodeDependencies)
    }

    private fun saveCacheBitcodeDependencies(immediateBitcodeDependencies: List<DependenciesTracker.ResolvedDependency>) {
        outputFiles.bitcodeDependenciesFile.writeLines(DependenciesSerializer.serialize(immediateBitcodeDependencies))
    }

    private fun saveInlineFunctionBodies(inlineFunctionBodies: List<SerializedInlineFunctionReference>) {
        outputFiles.inlineFunctionBodiesFile.writeBytes(InlineFunctionBodyReferenceSerializer.serialize(inlineFunctionBodies))
    }

    private fun saveClassFields(classFields: List<SerializedClassFields>) {
        outputFiles.classFieldsFile.writeBytes(ClassFieldsSerializer.serialize(classFields))
    }

    private fun saveEagerInitializedProperties(eagerInitializedFiles: List<SerializedEagerInitializedFile>) {
        outputFiles.eagerInitializedPropertiesFile.writeBytes(EagerInitializedPropertySerializer.serialize(eagerInitializedFiles))
    }
}


data class CacheAdditionalInfo(
        val inlineFunctionBodies: List<SerializedInlineFunctionReference> = emptyList(),
        val classFields: List<SerializedClassFields> = emptyList(),
        val eagerInitializedFiles: List<SerializedEagerInitializedFile> = emptyList(),
)