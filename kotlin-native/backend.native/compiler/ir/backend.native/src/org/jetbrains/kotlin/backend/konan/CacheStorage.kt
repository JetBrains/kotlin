/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadata
import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadataSerializer
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.EagerInitializedPropertySerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import org.jetbrains.kotlin.backend.konan.serialization.TrivialGettersSerializer
import org.jetbrains.kotlin.backend.konan.util.compilerFingerprint
import org.jetbrains.kotlin.backend.konan.util.runtimeFingerprint
import org.jetbrains.kotlin.konan.config.cachedLibraryDependenciesFingerprint
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.isNativeStdlib
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.bufferedWriter
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.writeBytes
import kotlin.io.path.writeLines
import kotlin.random.Random

private fun NativeGenerationState.generateCacheMetadata(): CacheMetadata {
    val runtimeFingerprint = if (config.libraryToCache!!.klib.isNativeStdlib) {
        config.distribution.runtimeFingerprint(config.target)
    } else {
        null
    }
    return CacheMetadata(
            hash = klibHash,
            host = HostManager.host,
            target = config.target,
            compilerFingerprint = config.distribution.compilerFingerprint,
            runtimeFingerprint = runtimeFingerprint,
            dependenciesFingerprint = config.configuration.cachedLibraryDependenciesFingerprint?.let { FingerprintHash.fromString(it) },
    )
}

internal class CacheStorage(private val generationState: NativeGenerationState) {
    private val outputFiles = generationState.outputFiles

    companion object {
        @OptIn(ExperimentalPathApi::class)
        fun renameOutput(outputFiles: OutputFiles, overwrite: Boolean) {
            if (outputFiles.mainFile.exists()) {
                if (!overwrite) {
                    outputFiles.tempCacheDirectory!!.deleteRecursively()
                    return
                }
                // For caches the output file is a directory. It might be already created,
                // we have to delete it in order for the next renaming operation to succeed.
                val tempDirectoryForRemoval = File(outputFiles.mainFileName + "-to-remove" + Random.nextLong())
                if (!outputFiles.mainFile.toFile().renameTo(tempDirectoryForRemoval))
                    return
                tempDirectoryForRemoval.deleteRecursively()
            }
            if (!outputFiles.tempCacheDirectory!!.toFile().renameTo(outputFiles.mainFile.toFile()))
                outputFiles.tempCacheDirectory.deleteRecursively()
        }
    }

    fun saveAdditionalCacheInfo() {
        outputFiles.prepareTempDirectories()
        if (!generationState.config.produce.isHeaderCache) {
            saveMetadata()
        }
        saveInlineFunctionBodies()
        saveCacheBitcodeDependencies()
        saveClassFields()
        saveEagerInitializedProperties()
        saveTrivialGetters()
    }

    private fun saveMetadata() {
        outputFiles.cacheMetadata!!.bufferedWriter().use {
            CacheMetadataSerializer.serialize(it, generationState.generateCacheMetadata())
        }
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

    private fun saveTrivialGetters() {
        outputFiles.trivialGettersFile!!.writeBytes(
                TrivialGettersSerializer.serialize(generationState.trivialGetters))
    }
}
