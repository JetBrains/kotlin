/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.konan.serialization.CacheDeserializationStrategy
import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadata
import org.jetbrains.kotlin.backend.konan.serialization.CacheMetadataSerializer
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.EagerInitializedPropertySerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import org.jetbrains.kotlin.backend.konan.serialization.ObjCAdapterSerializer
import org.jetbrains.kotlin.backend.konan.serialization.PartialLinkageIssuesSerializer
import org.jetbrains.kotlin.backend.konan.serialization.TrivialGettersSerializer
import org.jetbrains.kotlin.backend.konan.util.compilerFingerprint
import org.jetbrains.kotlin.backend.konan.util.runtimeFingerprint
import org.jetbrains.kotlin.konan.config.cachedLibraryDependenciesFingerprint
import org.jetbrains.kotlin.konan.config.filesToCache
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
        saveObjCAdapters()
        savePartialLinkageIssues()
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

    private fun saveObjCAdapters() {
        outputFiles.objCAdaptersFile!!.writeBytes(ObjCAdapterSerializer.serialize(generationState.objCAdapters))
    }

    /**
     * Stores the partial linkage issues reported for the code being cached, so that the compilations that reuse
     * this cache instead of compiling the code once again can report the very same issues (KT-78253).
     *
     * An issue may only be stored in the cache of a file that is rebuilt by every change which could resolve that
     * issue - otherwise the issue would outlive the problem it reports. Hence:
     * - An issue reported for one of the files being cached is stored in the cache of that very file. Whatever
     *   fixes such an issue - an edit of the file itself or of a declaration it references - makes that file dirty.
     * - An issue reported for anything else (e.g. for an inline function of a dependency, whose body is
     *   deserialized lazily, or for a missing declaration) is stored only if this compilation caches a single file,
     *   which is then the only code that could have triggered the issue. When several files are cached at once,
     *   there is no way to tell which of them is responsible: keeping the issue in the caches of all of them would
     *   make the issue survive in the caches of the innocent files, so it is not stored at all.
     */
    private fun savePartialLinkageIssues() {
        // Note: Empty for a whole-library cache build, in which case the deserialization strategy covers every file.
        val filePathsBeingCached = generationState.config.configuration.filesToCache
        val deserializationStrategy = generationState.cacheDeserializationStrategy
        val moduleName = generationState.context.irLinker
                .findKonanModuleDeserializer(generationState.config.libraryToCache!!.klib)?.moduleFragment?.name?.asString()

        val issues = generationState.config.partialLinkageIssues.collectResult().filter { issue ->
            // Different modules may contain the same source path, especially when KLIB paths are relative.
            val isFromCachedModule = issue.moduleName == moduleName
            when {
                // A whole-library cache also owns issues triggered by lazily deserialized dependency code.
                deserializationStrategy == CacheDeserializationStrategy.WholeModule -> true
                // The issue is reported for the code that is being cached here.
                isFromCachedModule && deserializationStrategy?.contains(issue.filePath) == true -> true
                // The issue belongs to the cache of another file compiled in this very compilation.
                isFromCachedModule && issue.filePath in filePathsBeingCached -> false
                // The issue is attributed to none of the files being cached.
                else -> filePathsBeingCached.size == 1
            }
        }

        outputFiles.partialLinkageIssuesFile!!.writeBytes(PartialLinkageIssuesSerializer.serialize(issues))
    }
}
