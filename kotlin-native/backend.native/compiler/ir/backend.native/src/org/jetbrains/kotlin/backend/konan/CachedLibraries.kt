/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.konan.CacheSupport.Companion.cacheFileId
import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.common.lazyEvaluatedFingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.io.canonicalPathString
import org.jetbrains.kotlin.io.listDirectoryEntriesIfDirectoryExists
import org.jetbrains.kotlin.konan.config.filesToCache
import org.jetbrains.kotlin.konan.library.isImplicitlyLoadedFromKotlinNativeDistribution
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.newCompanionInitializationEnabled
import org.jetbrains.kotlin.library.uniqueName
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readLines

private class LibraryHashComputer {
    private val hashes = mutableListOf<FingerprintHash>()

    fun update(hash: FingerprintHash) {
        hashes.add(hash)
    }

    fun digest() = FingerprintHash(hashes.fold(Hash128Bits(hashes.size.toULong())) { acc, x -> acc.combineWith(x.hash) })
}

private fun getArtifactName(target: KonanTarget, baseName: String, kind: CompilerOutputKind) =
        "${kind.prefix(target)}$baseName${kind.suffix(target)}"

class CachedLibraries(
        private val configuration: CompilerConfiguration,
        private val target: KonanTarget,
        private val compilerFingerprint: String,
        klibDag: KlibDAG,
        explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<Path>,
        autoCacheDirectory: Path,
        autoCacheableFrom: List<Path>,
        private val libraryToCache: KotlinLibrary?,
) {
    enum class Kind { DYNAMIC, STATIC, HEADER }

    sealed class Cache(protected val target: KonanTarget, val kind: Kind, val path: String, val rootDirectory: String) {
        val bitcodeDependencies by lazy { computeBitcodeDependencies() }
        val binariesPaths by lazy { computeBinariesPaths() }
        val serializedInlineFunctionBodies by lazy { computeSerializedInlineFunctionBodies() }
        val serializedClassFields by lazy { computeSerializedClassFields() }
        val serializedEagerInitializedFiles by lazy { computeSerializedEagerInitializedFiles() }
        val serializedTrivialGetters by lazy { computeSerializedTrivialGetters() }
        val serializedObjCAdapters by lazy { computeSerializedObjCAdapters() }

        protected abstract fun computeBitcodeDependencies(): List<DependenciesTracker.UnresolvedDependency>
        protected abstract fun computeBinariesPaths(): List<String>
        protected abstract fun computeSerializedInlineFunctionBodies(): List<SerializedInlineFunctionReference>
        protected abstract fun computeSerializedClassFields(): List<SerializedClassFields>
        protected abstract fun computeSerializedEagerInitializedFiles(): List<SerializedEagerInitializedFile>
        protected abstract fun computeSerializedTrivialGetters(): List<SerializedTrivialGetter>
        protected abstract fun computeSerializedObjCAdapters(): List<SerializedObjCAdapter>

        protected fun Kind.toCompilerOutputKind(): CompilerOutputKind = when (this) {
            Kind.DYNAMIC -> CompilerOutputKind.DYNAMIC_CACHE
            Kind.STATIC -> CompilerOutputKind.STATIC_CACHE
            Kind.HEADER -> CompilerOutputKind.HEADER_CACHE
        }

        /**
         * The partial linkage issues that have been recorded at the moment this cache was built: the ones recorded
         * for the cached file [fileId] in a per-file cache, or the ones recorded for the whole library in a monolithic
         * cache (then [fileId] is `null`). See KT-78253 for the details.
         *
         * Note: The file is absent when no issues have been recorded.
         */
        fun getPartialLinkageIssues(fileId: String?): List<SerializedPartialLinkageIssue> {
            val directory = Path(rootDirectory).let { if (fileId != null) it.resolve(fileId) else it }
            val file = directory.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(PARTIAL_LINKAGE_ISSUES_FILE_NAME)
            if (!file.exists()) return emptyList()
            return mutableListOf<SerializedPartialLinkageIssue>().also {
                PartialLinkageIssuesSerializer.deserializeTo(file.readBytes(), it)
            }
        }

        // Returns null when the metadata file is absent, which is the case for caches produced by compilers older than 2.2.20 (KT-87202).
        protected fun readMetadataOrNull(directory: Path): CacheMetadata? {
            val metadataFile = directory.resolve(METADATA_FILE_NAME)
            if (!metadataFile.exists()) return null
            return metadataFile.bufferedReader().use {
                CacheMetadataSerializer.deserialize(it)
            }
        }

        class Monolithic(target: KonanTarget, kind: Kind, path: String)
            : Cache(target, kind, path, Path(path).parent.parent.absolutePathString())
        {
            fun getMetadataOrNull(): CacheMetadata? = readMetadataOrNull(Path(rootDirectory))

            override fun computeBitcodeDependencies(): List<DependenciesTracker.UnresolvedDependency> {
                val directory = Path(path).absolute().parent
                val data = directory.resolve(BITCODE_DEPENDENCIES_FILE_NAME).readLines()
                return DependenciesSerializer.deserialize(path, data)
            }

            override fun computeBinariesPaths() = listOf(path)

            override fun computeSerializedInlineFunctionBodies() = mutableListOf<SerializedInlineFunctionReference>().also {
                val directory = Path(path).absolute().parent.parent
                val data = directory.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(INLINE_FUNCTION_BODIES_FILE_NAME).readBytes()
                InlineFunctionBodyReferenceSerializer.deserializeTo(data, it)
            }

            override fun computeSerializedClassFields() = mutableListOf<SerializedClassFields>().also {
                val directory = Path(path).absolute().parent.parent
                val data = directory.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(CLASS_FIELDS_FILE_NAME).readBytes()
                ClassFieldsSerializer.deserializeTo(data, it)
            }

            override fun computeSerializedEagerInitializedFiles() = mutableListOf<SerializedEagerInitializedFile>().also {
                val directory = Path(path).absolute().parent.parent
                val data = directory.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(EAGER_INITIALIZED_PROPERTIES_FILE_NAME).readBytes()
                EagerInitializedPropertySerializer.deserializeTo(data, it)
            }

            override fun computeSerializedTrivialGetters() = mutableListOf<SerializedTrivialGetter>().also {
                val directory = Path(path).absolute().parent.parent
                val data = directory.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(TRIVIAL_GETTERS_FILE_NAME).readBytes()
                TrivialGettersSerializer.deserializeTo(data, it)
            }

            override fun computeSerializedObjCAdapters() = mutableListOf<SerializedObjCAdapter>().also {
                val directory = Path(path).absolute().parent.parent
                val file = directory.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(OBJC_ADAPTERS_FILE_NAME)
                ObjCAdapterSerializer.deserializeTo(file.readBytes(), it)
            }
        }

        class PerFile(target: KonanTarget, kind: Kind, path: String, fileDirs: List<Path>, val complete: Boolean)
            : Cache(target, kind, path, Path(path).absolutePathString())
        {
            private val existingFileDirs = if (complete) fileDirs else fileDirs.filter { it.exists() }

            val fileIds: List<String> get() = existingFileDirs.map { it.name }

            private val perFileBitcodeDependencies by lazy {
                existingFileDirs.associate {
                    val data = it.resolve(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).resolve(BITCODE_DEPENDENCIES_FILE_NAME).readLines()
                    it.name to DependenciesSerializer.deserialize(it.absolutePathString(), data)
                }
            }

            fun getFileDependencies(file: String) =
                    perFileBitcodeDependencies[file] ?: error("File $file is not found in cache $path")

            fun getFileBinaryPath(file: String) =
                    Path(path).resolve(file).resolve(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).resolve(getArtifactName(target, file, kind.toCompilerOutputKind())).let {
                        require(it.exists()) { "File $file is not found in cache $path" }
                        it.absolutePathString()
                    }

            fun getMetadataOrNull(file: String): CacheMetadata? = readMetadataOrNull(Path(path).resolve(file))

            override fun computeBitcodeDependencies() = perFileBitcodeDependencies.values.flatten()

            override fun computeBinariesPaths() = existingFileDirs.map {
                it.resolve(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME).resolve(getArtifactName(target, it.name, kind.toCompilerOutputKind())).absolutePathString()
            }

            override fun computeSerializedInlineFunctionBodies() = mutableListOf<SerializedInlineFunctionReference>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(INLINE_FUNCTION_BODIES_FILE_NAME).readBytes()
                    InlineFunctionBodyReferenceSerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedClassFields() = mutableListOf<SerializedClassFields>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(CLASS_FIELDS_FILE_NAME).readBytes()
                    ClassFieldsSerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedEagerInitializedFiles() = mutableListOf<SerializedEagerInitializedFile>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(EAGER_INITIALIZED_PROPERTIES_FILE_NAME).readBytes()
                    EagerInitializedPropertySerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedTrivialGetters() = mutableListOf<SerializedTrivialGetter>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(TRIVIAL_GETTERS_FILE_NAME).readBytes()
                    TrivialGettersSerializer.deserializeTo(data, it)
                }
            }

            override fun computeSerializedObjCAdapters() = mutableListOf<SerializedObjCAdapter>().also {
                existingFileDirs.forEach { fileDir ->
                    val file = fileDir.resolve(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).resolve(OBJC_ADAPTERS_FILE_NAME)
                    ObjCAdapterSerializer.deserializeTo(file.readBytes(), it)
                }
            }
        }
    }

    private fun Path.trySelectCacheFor(library: KotlinLibrary): Cache? {
        // See Linker.renameOutput why is it ok to have an empty cache directory.
        val cacheDirContents = listDirectoryEntriesIfDirectoryExists().map { it.absolutePathString() }.toSet()
        if (cacheDirContents.isEmpty()) return null
        val cacheBinaryPartDir = resolve(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)
        val cacheBinaryPartDirContents = cacheBinaryPartDir.listDirectoryEntriesIfDirectoryExists().map { it.absolutePathString() }.toSet()
        val baseName = getCachedLibraryName(library)
        val dynamicFile = cacheBinaryPartDir.resolve(getArtifactName(target, baseName, CompilerOutputKind.DYNAMIC_CACHE))
        val staticFile = cacheBinaryPartDir.resolve(getArtifactName(target, baseName, CompilerOutputKind.STATIC_CACHE))
        val headerFile = cacheBinaryPartDir.resolve(getArtifactName(target, baseName, CompilerOutputKind.HEADER_CACHE))

        if (dynamicFile.absolutePathString() in cacheBinaryPartDirContents && staticFile.absolutePathString() in cacheBinaryPartDirContents)
            error("Both dynamic and static caches files cannot be in the same directory." +
                    " Library: ${library.path}, path to cache: ${absolutePathString()}")
        val perFileCache: Cache.PerFile by lazy {
            // When the per-file cache of a library is being rebuilt in parallel (one fragment per dirty file),
            // FinalizeCachePhase renames each file dir atomically over the old one, producing a brief window
            // during which the main dir does not exist. A sibling fragment iterating existingFileDirs to read
            // ir/{class_fields,inline_bodies,eager_init} would then throw NoSuchFileException. The cached data
            // for those files is stale anyway (the dirty file is loaded as IR), so skip them entirely.
            val filesToCache = configuration.filesToCache
            val fileIdsToCache = libraryToCache?.takeIf { it == library }?.getFileFqNames(filesToCache)?.let { fqNames ->
                filesToCache.zip(fqNames) { filePath, fqName -> cacheFileId(fqName, filePath) }.toSet()
            } ?: emptySet()
            val libraryFileDirs = library.getFilesWithFqNames().map { (filePath, fqName) ->
                resolve(cacheFileId(fqName, filePath))
            }
            Cache.PerFile(target, Kind.STATIC, absolutePathString(),
                    libraryFileDirs.filterNot { it.name in fileIdsToCache },
                    complete = cacheDirContents.containsAll(libraryFileDirs.map { it.absolutePathString() }))
        }
        return when {
            dynamicFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.DYNAMIC, dynamicFile.absolutePathString())
            staticFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.STATIC, staticFile.absolutePathString())
            // A metadata-only (header) cache yields to the per-file caches of the same library built into the
            // same directory: the latter carry native code, while the header cache carries metadata only
            // (see CacheStorage and CacheBuilder.staleCacheReason).
            headerFile.absolutePathString() in cacheBinaryPartDirContents && perFileCache.fileIds.isEmpty() ->
                    Cache.Monolithic(target, Kind.HEADER, headerFile.absolutePathString())
            else -> {
                perFileCache
            }
        }
    }

    private val cacheNameToImplicitDirMapping: Map<String, Path> =
            implicitCacheDirectories.flatMap { dir -> dir.listDirectoryEntriesIfDirectoryExists().map { it.name to it } }
                    .toMap()

    private fun KotlinLibrary.trySelectCacheAt(dirBuilder: (String) -> Path?) =
            sequenceOf(getPerFileCachedLibraryName(this), getCachedLibraryName(this))
                    .map(dirBuilder)
                    .mapNotNull { it?.trySelectCacheFor(this) }
                    .firstOrNull()

    private val allCaches: Map<KotlinLibrary, Cache> = klibDag.librariesReverseTopoSorted.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            Path(explicitPath).trySelectCacheFor(library)
                    ?: error("No cache found for library ${library.path} at $explicitPath")
        } else {
            val libraryPath = library.path.canonicalPathString()
            library.trySelectCacheAt { cacheNameToImplicitDirMapping[it] }
                    ?: autoCacheDirectory.takeIf { autoCacheableFrom.any { libraryPath.startsWith(it.canonicalPathString()) } }
                            ?.let {
                                val dir = computeLibraryCacheDirectory(it, library, klibDag)
                                library.trySelectCacheAt { cacheName -> dir.resolve(cacheName) }
                            }
        }

        cache?.let {
            // A safety measure. We don't expect the compiler to produce non-stdlib caches on MinGW.
            // However, if it does, we are going to be aware without breaking the compilation.
            if (target == KonanTarget.MINGW_X64 && !library.isNativeStdlib) {
                configuration.report(CliDiagnostics.KONAN_ARGUMENT_WARNING,
                        "MinGW target does not support caches for libraries except for stdlib. Found cache at ${cache.path}"
                )
                null
            } else {
                val incompatibilityReason = cacheIncompatibilityReason(library, it)
                if (incompatibilityReason != null) {
                    // Never mix an incompatible metadata/binary combination into a compilation: a cache with
                    // stale metadata (or a binary of a different compiler or target) would silently corrupt
                    // the result. Drop the cache instead: the library is then either compiled from its klib
                    // into the current artifact, or its cache is rebuilt (see CacheBuilder).
                    configuration.report(CliDiagnostics.KONAN_ARGUMENT_WARNING,
                            "Ignoring the cache for library ${library.path} at ${it.path}: $incompatibilityReason"
                    )
                    null
                } else {
                    library to it
                }
            }
        }
    }.toMap()

    /**
     * A human-readable reason why [cache] cannot be used for [library] in this compilation, or `null` if the cache is usable.
     *
     * A cache is an artifact of a particular (library content, compiler, host, target) combination, and a
     * metadata-only (header) cache additionally carries the exact metadata the consumers agree on. A cache that
     * does not match the current compilation must never be used: mixing its stale metadata into freshly compiled
     * code (or its foreign binary into the final link) silently corrupts the result. An incompatible cache is
     * dropped with a warning instead.
     *
     * Note: caches produced by compilers older than 2.2.20 carry no metadata at all (KT-87202). Such full caches
     * are still accepted for backward compatibility. A metadata-only (header) cache is not: it has only ever
     * been produced together with its identity, and an unidentified metadata artifact must not be trusted.
     *
     * Note: the fingerprint of the dependencies a cache was built against is verified by the incremental
     * machinery only (see [CacheBuilder]); a standalone compilation has no reliable way to recompute it.
     */
    private fun cacheIncompatibilityReason(library: KotlinLibrary, cache: Cache): String? {
        val metadata = when (cache) {
            is Cache.Monolithic -> cache.getMetadataOrNull()
            is Cache.PerFile -> cache.fileIds.firstOrNull()?.let { cache.getMetadataOrNull(it) }
        }
        if (metadata == null) {
            return if (cache.kind == Kind.HEADER) "it has no metadata, so its identity cannot be verified" else null
        }

        // The same memoized fingerprint the auto-cache lookup computes (see [computeLibraryCacheDirectory]).
        fun klibContentMismatch() = library.lazyEvaluatedFingerprintHash != metadata.hash

        return when {
            metadata.target != target ->
                    "it was produced for target ${metadata.target}"
            metadata.host != HostManager.host ->
                    "it was produced on host ${metadata.host}"
            metadata.compilerFingerprint != compilerFingerprint ->
                    "it was produced by a different compiler version"
            metadata.newCompanionInitialization != library.newCompanionInitializationEnabled ->
                    "it was produced with a different companion initialization order"
            // The metadata of a header cache and the binary of a full cache both must describe the klib at hand.
            // The full monolithic caches of non-distribution libraries are checked as well: such a cache is
            // normally located by name only, so a changed library would silently reuse the stale cache.
            // The distribution libraries (including stdlib) only change together with the compiler, which the
            // compiler fingerprint check above already covers; the per-file caches are the incremental
            // machinery's responsibility (see CacheBuilder.staleCacheReason).
            cache is Cache.Monolithic
                    && (cache.kind == Kind.HEADER || (!library.isImplicitlyLoadedFromKotlinNativeDistribution && !library.isNativeStdlib))
                    && klibContentMismatch() ->
                    "it does not match the current content of the library"
            else -> null
        }
    }

    fun isLibraryCached(library: KotlinLibrary, allowIncomplete: Boolean = false): Boolean =
            getLibraryCache(library, allowIncomplete) != null

    fun getLibraryCache(library: KotlinLibrary, allowIncomplete: Boolean = false): Cache? =
            allCaches[library]?.takeIf { allowIncomplete || (it as? Cache.PerFile)?.complete != false }

    val hasStaticCaches = allCaches.values.any {
        when (it.kind) {
            Kind.STATIC -> true
            else -> false
        }
    }

    val hasDynamicCaches = allCaches.values.any {
        when (it.kind) {
            Kind.DYNAMIC -> true
            else -> false
        }
    }

    companion object {
        fun getPerFileCachedLibraryName(library: KotlinLibrary): String = "${library.uniqueName}-per-file-cache"
        fun getCachedLibraryName(library: KotlinLibrary): String = getCachedLibraryName(library.uniqueName)
        fun getCachedLibraryName(libraryName: String): String = "$libraryName-cache"

        fun computeDependenciesFingerprint(
                dependencies: List<KotlinLibrary>,
        ): FingerprintHash {
            val hashComputer = LibraryHashComputer()
            dependencies.sortedBy { it.uniqueName }.forEach {
                hashComputer.update(it.lazyEvaluatedFingerprintHash)
            }
            return hashComputer.digest()
        }

        fun computeLibraryCacheDirectory(
                baseCacheDirectory: Path,
                library: KotlinLibrary,
                klibDag: KlibDAG,
        ): Path {
            val dependencies = klibDag.getAllDependencies(library)
            val fingerprintHash = computeDependenciesFingerprint(listOf(library) + dependencies)
            return baseCacheDirectory.resolve(library.uniqueName).resolve(fingerprintHash.toString())
        }

        const val PER_FILE_CACHE_IR_LEVEL_DIR_NAME = "ir"
        const val PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME = "bin"

        const val METADATA_FILE_NAME = "metadata.properties"
        const val BITCODE_DEPENDENCIES_FILE_NAME = "bitcode_deps"
        const val INLINE_FUNCTION_BODIES_FILE_NAME = "inline_bodies"
        const val CLASS_FIELDS_FILE_NAME = "class_fields"
        const val EAGER_INITIALIZED_PROPERTIES_FILE_NAME = "eager_init"
        const val TRIVIAL_GETTERS_FILE_NAME = "trivial_getters"
        const val OBJC_ADAPTERS_FILE_NAME = "objc_adapters"
        const val PARTIAL_LINKAGE_ISSUES_FILE_NAME = "pl_issues"
    }
}
