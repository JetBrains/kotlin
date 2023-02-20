/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.EagerInitializedPropertySerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import java.io.File
import java.nio.file.Files

internal class CacheStorage(
        private val cacheRootDirectory: File
) {

    private fun File.cacheBinaryPart() = File(this, CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)

    private fun File.cacheIrPart() = File(this, CachedLibraries.PER_FILE_CACHE_IR_LEVEL_DIR_NAME)

    companion object {
        fun renameOutput(temporaryCacheDirectory: File, finalCacheDirectory: File) {
            // For caches the output file is a directory. It might be created by someone else,
            // we have to delete it in order for the next renaming operation to succeed.
            // TODO: what if the directory is not empty?
            finalCacheDirectory.delete()
            if (!temporaryCacheDirectory.renameTo(finalCacheDirectory))
                temporaryCacheDirectory.deleteRecursively()
        }
    }

    fun saveAdditionalCacheInfo(
            immediateBitcodeDependencies: List<DependenciesTracker.ResolvedDependency>,
            inlineFunctionBodies: List<SerializedInlineFunctionReference>,
            classFields: List<SerializedClassFields>,
            eagerInitializedFiles: List<SerializedEagerInitializedFile>,
    ) {
        cacheRootDirectory.mkdirs()
        cacheRootDirectory.cacheBinaryPart().mkdirs()
        cacheRootDirectory.cacheIrPart().mkdirs()
        saveCacheBitcodeDependencies(immediateBitcodeDependencies)
        saveInlineFunctionBodies(inlineFunctionBodies)
        saveClassFields(classFields)
        saveEagerInitializedProperties(eagerInitializedFiles)
    }

    private fun saveCacheBitcodeDependencies(immediateBitcodeDependencies: List<DependenciesTracker.ResolvedDependency>) {
        val bitcodeDependenciesFile = cacheRootDirectory.cacheBinaryPart().resolve(CachedLibraries.BITCODE_DEPENDENCIES_FILE_NAME)
        Files.write(bitcodeDependenciesFile.toPath(), DependenciesSerializer.serialize(immediateBitcodeDependencies))
    }

    private fun saveInlineFunctionBodies(inlineFunctionBodies: List<SerializedInlineFunctionReference>) {
        val inlineFunctionBodiesFile = cacheRootDirectory.cacheIrPart().resolve(CachedLibraries.INLINE_FUNCTION_BODIES_FILE_NAME)
        inlineFunctionBodiesFile.writeBytes(InlineFunctionBodyReferenceSerializer.serialize(inlineFunctionBodies))
    }

    private fun saveClassFields(classFields: List<SerializedClassFields>) {
        val classFieldsFile = cacheRootDirectory.cacheIrPart().resolve(CachedLibraries.CLASS_FIELDS_FILE_NAME)
        classFieldsFile.writeBytes(ClassFieldsSerializer.serialize(classFields))
    }

    private fun saveEagerInitializedProperties(eagerInitializedFiles: List<SerializedEagerInitializedFile>) {
        val eagerInitializedPropertiesFile = cacheRootDirectory.cacheIrPart().resolve(CachedLibraries.EAGER_INITIALIZED_PROPERTIES_FILE_NAME)
        eagerInitializedPropertiesFile.writeBytes(EagerInitializedPropertySerializer.serialize(eagerInitializedFiles))
    }
}
