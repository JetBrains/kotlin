/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.library.uniqueName

private class LibraryHashComputer {
    private val hashes = mutableListOf<FingerprintHash>()

    fun update(hash: FingerprintHash) {
        hashes.add(hash)
    }

    fun digest() = FingerprintHash(hashes.fold(Hash128Bits(hashes.size.toULong())) { acc, x -> acc.combineWith(x.hash) })
}

private fun LibraryHashComputer.digestLibrary(library: KotlinLibrary) =
        update(SerializedKlibFingerprint(library.libraryFile.javaFile()).klibFingerprint)

private fun getArtifactName(target: KonanTarget, baseName: String, kind: CompilerOutputKind) =
        "${kind.prefix(target)}$baseName${kind.suffix(target)}"

class CachedLibraries(
        private val target: KonanTarget,
        allLibraries: List<KotlinLibrary>,
        explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<File>,
        autoCacheDirectory: File,
        autoCacheableFrom: List<File>
) {
    enum class Kind { DYNAMIC, STATIC }

    sealed class Cache(protected val target: KonanTarget, val kind: Kind, val path: String, val rootDirectory: String) {
        val bitcodeDependencies by lazy { computeBitcodeDependencies() }
        val binariesPaths by lazy { computeBinariesPaths() }
        val serializedInlineFunctionBodies by lazy { computeSerializedInlineFunctionBodies() }
        val serializedClassFields by lazy { computeSerializedClassFields() }
        val serializedEagerInitializedFiles by lazy { computeSerializedEagerInitializedFiles() }

        protected abstract fun computeBitcodeDependencies(): List<DependenciesTracker.UnresolvedDependency>
        protected abstract fun computeBinariesPaths(): List<String>
        protected abstract fun computeSerializedInlineFunctionBodies(): List<SerializedInlineFunctionReference>
        protected abstract fun computeSerializedClassFields(): List<SerializedClassFields>
        protected abstract fun computeSerializedEagerInitializedFiles(): List<SerializedEagerInitializedFile>

        protected fun Kind.toCompilerOutputKind(): CompilerOutputKind = when (this) {
            Kind.DYNAMIC -> CompilerOutputKind.DYNAMIC_CACHE
            Kind.STATIC -> CompilerOutputKind.STATIC_CACHE
        }

        class Monolithic(target: KonanTarget, kind: Kind, path: String)
            : Cache(target, kind, path, File(path).parentFile.parentFile.absolutePath)
        {
            override fun computeBitcodeDependencies(): List<DependenciesTracker.UnresolvedDependency> {
                val directory = File(path).absoluteFile.parentFile
                val data = directory.child(BITCODE_DEPENDENCIES_FILE_NAME).readStrings()
                return DependenciesSerializer.deserialize(path, data)
            }

            override fun computeBinariesPaths() = listOf(path)

            override fun computeSerializedInlineFunctionBodies() = mutableListOf<SerializedInlineFunctionReference>().also {
                val directory = File(path).absoluteFile.parentFile.parentFile
                val data = directory.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(INLINE_FUNCTION_BODIES_FILE_NAME).readBytes()
                InlineFunctionBodyReferenceSerializer.deserializeTo(data, it)
            }

            override fun computeSerializedClassFields() = mutableListOf<SerializedClassFields>().also {
                val directory = File(path).absoluteFile.parentFile.parentFile
                val data = directory.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(CLASS_FIELDS_FILE_NAME).readBytes()
                ClassFieldsSerializer.deserializeTo(data, it)
            }

            override fun computeSerializedEagerInitializedFiles() = mutableListOf<SerializedEagerInitializedFile>().also {
                val directory = File(path).absoluteFile.parentFile.parentFile
                val data = directory.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(EAGER_INITIALIZED_PROPERTIES_FILE_NAME).readBytes()
                EagerInitializedPropertySerializer.deserializeTo(data, it)
            }
        }

        class PerFile(target: KonanTarget, kind: Kind, path: String, fileDirs: List<File>, val complete: Boolean)
            : Cache(target, kind, path, File(path).absolutePath)
        {
            private val existingFileDirs = if (complete) fileDirs else fileDirs.filter { it.exists }

            private val perFileBitcodeDependencies by lazy {
                existingFileDirs.associate {
                    val data = it.child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).child(BITCODE_DEPENDENCIES_FILE_NAME).readStrings()
                    it.name to DependenciesSerializer.deserialize(it.absolutePath, data)
                }
            }

            fun getFileDependencies(file: String) =
                    perFileBitcodeDependencies[file] ?: error("File $file is not found in cache $path")

            fun getFileBinaryPath(file: String) =
                    File(path).child(file).child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).child(getArtifactName(target, file, kind.toCompilerOutputKind())).let {
                        require(it.exists) { "File $file is not found in cache $path" }
                        it.absolutePath
                    }

            fun getFileHash(file: String) =
                    File(path).child(file).child(HASH_FILE_NAME).readBytes()

            override fun computeBitcodeDependencies() = perFileBitcodeDependencies.values.flatten()

            override fun computeBinariesPaths() = existingFileDirs.map {
                it.child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).child(getArtifactName(target, it.name, kind.toCompilerOutputKind())).absolutePath
            }

            override fun computeSerializedInlineFunctionBodies() = mutableListOf<SerializedInlineFunctionReference>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(INLINE_FUNCTION_BODIES_FILE_NAME).readBytes()
                    InlineFunctionBodyReferenceSerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedClassFields() = mutableListOf<SerializedClassFields>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(CLASS_FIELDS_FILE_NAME).readBytes()
                    ClassFieldsSerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedEagerInitializedFiles() = mutableListOf<SerializedEagerInitializedFile>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(EAGER_INITIALIZED_PROPERTIES_FILE_NAME).readBytes()
                    EagerInitializedPropertySerializer.deserializeTo(data, it)
                }
            }
        }
    }

    private fun File.trySelectCacheFor(library: KotlinLibrary): Cache? {
        // See Linker.renameOutput why is it ok to have an empty cache directory.
        val cacheDirContents = listFilesOrEmpty.map { it.absolutePath }.toSet()
        if (cacheDirContents.isEmpty()) return null
        val cacheBinaryPartDir = child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)
        val cacheBinaryPartDirContents = cacheBinaryPartDir.listFilesOrEmpty.map { it.absolutePath }.toSet()
        val baseName = getCachedLibraryName(library)
        val dynamicFile = cacheBinaryPartDir.child(getArtifactName(target, baseName, CompilerOutputKind.DYNAMIC_CACHE))
        val staticFile = cacheBinaryPartDir.child(getArtifactName(target, baseName, CompilerOutputKind.STATIC_CACHE))

        if (dynamicFile.absolutePath in cacheBinaryPartDirContents && staticFile.absolutePath in cacheBinaryPartDirContents)
            error("Both dynamic and static caches files cannot be in the same directory." +
                    " Library: ${library.libraryName}, path to cache: $absolutePath")
        return when {
            dynamicFile.absolutePath in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.DYNAMIC, dynamicFile.absolutePath)
            staticFile.absolutePath in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.STATIC, staticFile.absolutePath)
            else -> {
                val libraryFileDirs = library.getFilesWithFqNames().map {
                    child(CacheSupport.cacheFileId(it.fqName, it.filePath))
                }
                Cache.PerFile(target, Kind.STATIC, absolutePath, libraryFileDirs,
                        complete = cacheDirContents.containsAll(libraryFileDirs.map { it.absolutePath }))
            }
        }
    }

    private val uniqueNameToLibrary = allLibraries.associateBy { it.uniqueName }
    private val uniqueNameToHash = mutableMapOf<String, FingerprintHash>()

    private val cacheNameToImplicitDirMapping: Map<String, File> =
            implicitCacheDirectories.flatMap { dir -> dir.listFilesOrEmpty.map { it.name to it } }
                    .toMap()

    private fun KotlinLibrary.trySelectCacheAt(dirBuilder: (String) -> File?) =
            sequenceOf(getPerFileCachedLibraryName(this), getCachedLibraryName(this))
                    .map(dirBuilder)
                    .mapNotNull { it?.trySelectCacheFor(this) }
                    .firstOrNull()

    private val allCaches: Map<KotlinLibrary, Cache> = allLibraries.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            File(explicitPath).trySelectCacheFor(library)
                    ?: error("No cache found for library ${library.libraryName} at $explicitPath")
        } else {
            val libraryPath = library.libraryFile.absolutePath
            library.trySelectCacheAt { cacheNameToImplicitDirMapping[it] }
                    ?: autoCacheDirectory.takeIf { autoCacheableFrom.any { libraryPath.startsWith(it.absolutePath) } }
                            ?.let {
                                val dir = computeVersionedCacheDirectory(it, library, uniqueNameToLibrary, uniqueNameToHash)
                                library.trySelectCacheAt { cacheName -> dir.child(cacheName) }
                            }
        }

        cache?.let { library to it }
    }.toMap()

    fun isLibraryCached(library: KotlinLibrary): Boolean =
            getLibraryCache(library) != null

    fun getLibraryCache(library: KotlinLibrary, allowIncomplete: Boolean = false): Cache? =
            allCaches[library]?.takeIf { allowIncomplete || (it as? Cache.PerFile)?.complete != false }

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

        private fun computeLibraryHash(library: KotlinLibrary, librariesHashes: MutableMap<String, FingerprintHash>) =
                librariesHashes.getOrPut(library.uniqueName) {
                    val hashComputer = LibraryHashComputer()
                    hashComputer.digestLibrary(library)
                    hashComputer.digest()
                }

        fun computeVersionedCacheDirectory(
                baseCacheDirectory: File,
                library: KotlinLibrary,
                allLibraries: Map<String, KotlinLibrary>,
                librariesHashes: MutableMap<String, FingerprintHash>,
        ): File {
            val dependencies = library.getAllTransitiveDependencies(allLibraries)
            val hashComputer = LibraryHashComputer()
            hashComputer.update(computeLibraryHash(library, librariesHashes))
            dependencies.sortedBy { it.uniqueName }.forEach {
                hashComputer.update(computeLibraryHash(it, librariesHashes))
            }

            val version = library.versions.libraryVersion ?: "unspecified"
            val hashString = hashComputer.digest().toString()
            return baseCacheDirectory.child(library.uniqueName).child(version).child(hashString)
        }

        const val PER_FILE_CACHE_IR_LEVEL_DIR_NAME = "ir"
        const val PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME = "bin"

        const val HASH_FILE_NAME = "hash"
        const val BITCODE_DEPENDENCIES_FILE_NAME = "bitcode_deps"
        const val INLINE_FUNCTION_BODIES_FILE_NAME = "inline_bodies"
        const val CLASS_FIELDS_FILE_NAME = "class_fields"
        const val EAGER_INITIALIZED_PROPERTIES_FILE_NAME = "eager_init"
    }
}
