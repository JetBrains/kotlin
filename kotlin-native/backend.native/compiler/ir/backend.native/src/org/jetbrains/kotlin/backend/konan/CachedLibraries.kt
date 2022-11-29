/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import org.jetbrains.kotlin.backend.konan.serialization.SerializedClassFields
import org.jetbrains.kotlin.backend.konan.serialization.SerializedInlineFunctionReference
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName

class CachedLibraries(
        private val target: KonanTarget,
        allLibraries: List<KotlinLibrary>,
        explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<File>
) {
    enum class Kind { DYNAMIC, STATIC }
    enum class Granularity { MODULE, FILE }

    private fun Kind.toCompilerOutputKind(): CompilerOutputKind = when (this) {
        Kind.DYNAMIC -> CompilerOutputKind.DYNAMIC_CACHE
        Kind.STATIC -> CompilerOutputKind.STATIC_CACHE
    }

    inner class Cache(val kind: Kind, val granularity: Granularity, val path: String) {
        val fileDirs by lazy { File(path).listFiles.filter { it.isDirectory }.sortedBy { it.name } }

        val bitcodeDependencies by lazy {
            when (granularity) {
                Granularity.MODULE -> File(path).parentFile.child(BITCODE_DEPENDENCIES_FILE_NAME).readStrings()
                Granularity.FILE -> fileDirs.flatMap {
                    it.child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).child(BITCODE_DEPENDENCIES_FILE_NAME).readStrings()
                }.distinct()
            }
        }

        val binariesPaths by lazy {
            when (granularity) {
                Granularity.MODULE -> listOf(path)
                Granularity.FILE -> fileDirs.map {
                    it.child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).child(getArtifactName(it.name, kind.toCompilerOutputKind())).absolutePath
                }
            }
        }

        val serializedInlineFunctionBodies by lazy {
            val result = mutableListOf<SerializedInlineFunctionReference>()
            when (granularity) {
                Granularity.MODULE -> {
                    val directory = File(path).absoluteFile.parentFile.parentFile
                    val data = directory.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(INLINE_FUNCTION_BODIES_FILE_NAME).readBytes()
                    InlineFunctionBodyReferenceSerializer.deserializeTo(data, result)
                }
                Granularity.FILE -> {
                    fileDirs.forEach { fileDir ->
                        val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(INLINE_FUNCTION_BODIES_FILE_NAME).readBytes()
                        InlineFunctionBodyReferenceSerializer.deserializeTo(data, result)
                    }
                }
            }
            result
        }

        val serializedClassFields by lazy {
            val result = mutableListOf<SerializedClassFields>()
            when (granularity) {
                Granularity.MODULE -> {
                    val directory = File(path).absoluteFile.parentFile.parentFile
                    val data = directory.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(CLASS_FIELDS_FILE_NAME).readBytes()
                    ClassFieldsSerializer.deserializeTo(data, result)
                }
                Granularity.FILE -> {
                    fileDirs.forEach { fileDir ->
                        val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(CLASS_FIELDS_FILE_NAME).readBytes()
                        ClassFieldsSerializer.deserializeTo(data, result)
                    }
                }
            }
            result
        }
    }

    private val cacheDirsContents = mutableMapOf<String, Set<String>>()

    private fun selectCache(library: KotlinLibrary, cacheDir: File): Cache? {
        // See Linker.renameOutput why is it ok to have an empty cache directory.
        val cacheDirContents = cacheDirsContents.getOrPut(cacheDir.absolutePath) {
            cacheDir.listFilesOrEmpty.map { it.absolutePath }.toSet()
        }
        if (cacheDirContents.isEmpty()) return null
        val cacheBinaryPartDir = cacheDir.child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)
        val cacheBinaryPartDirContents = cacheDirsContents.getOrPut(cacheBinaryPartDir.absolutePath) {
            cacheBinaryPartDir.listFilesOrEmpty.map { it.absolutePath }.toSet()
        }
        val baseName = getCachedLibraryName(library)
        val dynamicFile = cacheBinaryPartDir.child(getArtifactName(baseName, CompilerOutputKind.DYNAMIC_CACHE))
        val staticFile = cacheBinaryPartDir.child(getArtifactName(baseName, CompilerOutputKind.STATIC_CACHE))

        if (dynamicFile.absolutePath in cacheBinaryPartDirContents && staticFile.absolutePath in cacheBinaryPartDirContents)
            error("Both dynamic and static caches files cannot be in the same directory." +
                    " Library: ${library.libraryName}, path to cache: ${cacheDir.absolutePath}")
        return when {
            dynamicFile.absolutePath in cacheBinaryPartDirContents -> Cache(Kind.DYNAMIC, Granularity.MODULE, dynamicFile.absolutePath)
            staticFile.absolutePath in cacheBinaryPartDirContents -> Cache(Kind.STATIC, Granularity.MODULE, staticFile.absolutePath)
            else -> Cache(Kind.STATIC, Granularity.FILE, cacheDir.absolutePath)
        }
    }

    private val allCaches: Map<KotlinLibrary, Cache> = allLibraries.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            selectCache(library, File(explicitPath))
                    ?: error("No cache found for library ${library.libraryName} at $explicitPath")
        } else {
            implicitCacheDirectories.firstNotNullOfOrNull { dir ->
                selectCache(library, dir.child(getPerFileCachedLibraryName(library)))
                        ?: selectCache(library, dir.child(getCachedLibraryName(library)))
            }
        }

        cache?.let { library to it }
    }.toMap()

    private fun getArtifactName(baseName: String, kind: CompilerOutputKind) =
            "${kind.prefix(target)}$baseName${kind.suffix(target)}"

    fun isLibraryCached(library: KotlinLibrary): Boolean =
            getLibraryCache(library) != null

    fun getLibraryCache(library: KotlinLibrary): Cache? =
            allCaches[library]

    val hasStaticCaches = allCaches.values.any {
        when (it.kind) {
            Kind.STATIC -> true
            Kind.DYNAMIC -> false
        }
    }

    val hasDynamicCaches = allCaches.values.any {
        when (it.kind) {
            Kind.STATIC -> false
            Kind.DYNAMIC -> true
        }
    }

    companion object {
        fun getPerFileCachedLibraryName(library: KotlinLibrary): String = "${library.uniqueName}-per-file-cache"
        fun getCachedLibraryName(library: KotlinLibrary): String = getCachedLibraryName(library.uniqueName)
        fun getCachedLibraryName(libraryName: String): String = "$libraryName-cache"

        const val PER_FILE_CACHE_IR_LEVEL_DIR_NAME = "ir"
        const val PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME = "bin"

        const val BITCODE_DEPENDENCIES_FILE_NAME = "bitcode_deps"
        const val INLINE_FUNCTION_BODIES_FILE_NAME = "inline_bodies"
        const val CLASS_FIELDS_FILE_NAME = "class_fields"
    }
}
