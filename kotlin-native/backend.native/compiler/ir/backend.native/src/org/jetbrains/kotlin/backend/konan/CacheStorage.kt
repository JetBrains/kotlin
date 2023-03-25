/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.EagerInitializedPropertySerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer

internal class CacheStorage(private val generationState: NativeGenerationState) {
    private val outputFiles = generationState.outputFiles

    companion object {
        fun renameOutput(outputFiles: OutputFiles) {
            // For caches the output file is a directory. It might be created by someone else,
            // we have to delete it in order for the next renaming operation to succeed.
            // TODO: what if the directory is not empty?
            java.io.File(outputFiles.mainFileName).delete()
            if (!outputFiles.tempCacheDirectory!!.renameTo(outputFiles.mainFile))
                outputFiles.tempCacheDirectory.deleteRecursively()
        }
    }

    fun saveAdditionalCacheInfo() {
        outputFiles.prepareTempDirectories()
        saveCacheBitcodeDependencies()
        saveInlineFunctionBodies()
        saveClassFields()
        saveEagerInitializedProperties()
    }

    private fun saveCacheBitcodeDependencies() {
        outputFiles.bitcodeDependenciesFile!!.writeLines(
                DependenciesSerializer.serialize(generationState.dependenciesTracker.immediateBitcodeDependencies))
    }

    private fun saveInlineFunctionBodies() {
        outputFiles.inlineFunctionBodiesFile!!.writeBytes(
                InlineFunctionBodyReferenceSerializer.serialize(generationState.inlineFunctionBodies))
    }

    private fun saveClassFields() {
        outputFiles.classFieldsFile!!.writeBytes(
                ClassFieldsSerializer.serialize(generationState.classFields))
    }

    private fun saveEagerInitializedProperties() {
        outputFiles.eagerInitializedPropertiesFile!!.writeBytes(
                EagerInitializedPropertySerializer.serialize(generationState.eagerInitializedFiles))
    }
}
