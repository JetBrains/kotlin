/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.backend.konan.serialization.ClassFieldsSerializer
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionBodyReferenceSerializer
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import java.security.MessageDigest

private fun MessageDigest.digestFile(file: File) =
        if (file.isDirectory) digestDirectory(file) else update(file.readBytes())

private fun MessageDigest.digestDirectory(directory: File): Unit =
        directory.listFiles.sortedBy { it.name }.forEach { digestFile(it) }

private fun MessageDigest.digestLibrary(library: KotlinLibrary) = digestFile(library.libraryFile)

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

        class PerFile(target: KonanTarget, kind: Kind, path: String, private val fileDirs: List<File>)
            : Cache(target, kind, path, File(path).absolutePath)
        {
            private val perFileBitcodeDependencies by lazy {
                fileDirs.associate {
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

            override fun computeBitcodeDependencies() = perFileBitcodeDependencies.values.flatten()

            override fun computeBinariesPaths() = fileDirs.map {
                it.child(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).child(getArtifactName(target, it.name, kind.toCompilerOutputKind())).absolutePath
            }

            override fun computeSerializedInlineFunctionBodies() = mutableListOf<SerializedInlineFunctionReference>().also {
                fileDirs.forEach { fileDir ->
                    val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(INLINE_FUNCTION_BODIES_FILE_NAME).readBytes()
                    InlineFunctionBodyReferenceSerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedClassFields() = mutableListOf<SerializedClassFields>().also {
                fileDirs.forEach { fileDir ->
                    val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(CLASS_FIELDS_FILE_NAME).readBytes()
                    ClassFieldsSerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedEagerInitializedFiles() = mutableListOf<SerializedEagerInitializedFile>().also {
                fileDirs.forEach { fileDir ->
                    val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(EAGER_INITIALIZED_PROPERTIES_FILE_NAME).readBytes()
                    EagerInitializedPropertySerializer.deserializeTo(data, it)
                }
            }
        }
    }

    private val cacheDirsContents = mutableMapOf<String, Set<String>>()
    private val librariesFileDirs = mutableMapOf<KotlinLibrary, List<File>>()

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
        val dynamicFile = cacheBinaryPartDir.child(getArtifactName(target, baseName, CompilerOutputKind.DYNAMIC_CACHE))
        val staticFile = cacheBinaryPartDir.child(getArtifactName(target, baseName, CompilerOutputKind.STATIC_CACHE))

        if (dynamicFile.absolutePath in cacheBinaryPartDirContents && staticFile.absolutePath in cacheBinaryPartDirContents)
            error("Both dynamic and static caches files cannot be in the same directory." +
                    " Library: ${library.libraryName}, path to cache: ${cacheDir.absolutePath}")
        return when {
            dynamicFile.absolutePath in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.DYNAMIC, dynamicFile.absolutePath)
            staticFile.absolutePath in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.STATIC, staticFile.absolutePath)
            else -> {
                val libraryFileDirs = librariesFileDirs.getOrPut(library) {
                    library.getFilesWithFqNames().map { cacheDir.child(CacheSupport.cacheFileId(it.fqName, it.filePath)) }
                }
                Cache.PerFile(target, Kind.STATIC, cacheDir.absolutePath, libraryFileDirs)
                        .takeIf { cacheDirContents.containsAll(libraryFileDirs.map { it.absolutePath }) }
            }
        }
    }

    private val uniqueNameToLibrary = allLibraries.associateBy { it.uniqueName }

    private val allCaches: Map<KotlinLibrary, Cache> = allLibraries.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            selectCache(library, File(explicitPath))
                    ?: error("No cache found for library ${library.libraryName} at $explicitPath")
        } else {
            val libraryPath = library.libraryFile.absolutePath
            implicitCacheDirectories.firstNotNullOfOrNull { dir ->
                selectCache(library, dir.child(getPerFileCachedLibraryName(library)))
                        ?: selectCache(library, dir.child(getCachedLibraryName(library)))
            }
                    ?: autoCacheDirectory.takeIf { autoCacheableFrom.any { libraryPath.startsWith(it.absolutePath) } }
                            ?.let {
                                val dir = computeVersionedCacheDirectory(it, library, uniqueNameToLibrary)
                                selectCache(library, dir.child(getPerFileCachedLibraryName(library)))
                                        ?: selectCache(library, dir.child(getCachedLibraryName(library)))
                            }
        }

        cache?.let { library to it }
    }.toMap()

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

        @OptIn(ExperimentalUnsignedTypes::class)
        fun computeVersionedCacheDirectory(baseCacheDirectory: File, library: KotlinLibrary, allLibraries: Map<String, KotlinLibrary>): File {
            val dependencies = library.getAllTransitiveDependencies(allLibraries)
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(compilerMarker)
            messageDigest.digestLibrary(library)
            dependencies.sortedBy { it.uniqueName }.forEach { messageDigest.digestLibrary(it) }

            val version = library.versions.libraryVersion ?: "unspecified"
            val hashString = messageDigest.digest().asUByteArray()
                    .joinToString("") { it.toString(radix = 16).padStart(2, '0') }
            return baseCacheDirectory.child(library.uniqueName).child(version).child(hashString)
        }

        const val PER_FILE_CACHE_IR_LEVEL_DIR_NAME = "ir"
        const val PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME = "bin"

        const val BITCODE_DEPENDENCIES_FILE_NAME = "bitcode_deps"
        const val INLINE_FUNCTION_BODIES_FILE_NAME = "inline_bodies"
        const val CLASS_FIELDS_FILE_NAME = "class_fields"
        const val EAGER_INITIALIZED_PROPERTIES_FILE_NAME = "eager_init"

        // TODO: Remove after dropping Gradle cache orchestration.
        private val compilerMarker = "K/N orchestration".encodeToByteArray()
    }
}
