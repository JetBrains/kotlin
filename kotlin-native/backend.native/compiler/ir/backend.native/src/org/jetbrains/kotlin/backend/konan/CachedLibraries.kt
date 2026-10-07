/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.konan.CacheSupport.Companion.cacheFileId
import org.jetbrains.kotlin.backend.konan.library.KlibDAG
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportUnits
import org.jetbrains.kotlin.backend.common.lazyEvaluatedFingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.io.canonicalPathString
import org.jetbrains.kotlin.io.listDirectoryEntriesIfDirectoryExists
import org.jetbrains.kotlin.konan.config.filesToCache
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.uniqueName
import java.nio.file.Path
import java.util.Properties
import java.util.zip.GZIPInputStream
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists
import kotlin.io.path.inputStream
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
        private val klibDag: KlibDAG,
        explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<Path>,
        private val cacheDirectories: List<Path>,
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
        return when {
            dynamicFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.DYNAMIC, dynamicFile.absolutePathString())
            staticFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.STATIC, staticFile.absolutePathString())
            headerFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.HEADER, headerFile.absolutePathString())
            else -> {
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
        }
    }

    private val cacheNameToImplicitDirMapping: Map<String, Path> =
            implicitCacheDirectories.flatMap { dir -> dir.listDirectoryEntriesIfDirectoryExists().map { it.name to it } }
                    .toMap()

    class ObjCCache(val binaryPath: String, val metadata: ObjCCacheMetadata, val units: ObjCExportUnits)

    fun exportedDependencies(library: KotlinLibrary, exportedLibraries: Set<KotlinLibrary>): List<String> =
            klibDag.getAllDependencies(library)
                    .filter { it in exportedLibraries }
                    .map { it.uniqueName }
                    .sorted()

    fun getObjCCaches(exportedLibraries: Set<KotlinLibrary>, moduleName: String): Map<KotlinLibrary, ObjCCache> {
        if (exportedLibraries.isEmpty() || moduleName.isEmpty()) return emptyMap()
        val directoriesByName = cacheDirectories.flatMap { dir -> dir.listDirectoryEntriesIfDirectoryExists() }.groupBy { it.name }
        return exportedLibraries.mapNotNull { library ->
            val archive = findObjCCache(library, moduleName, directoriesByName) ?: return@mapNotNull null
            val directory = archive.parent.parent
            if (getLibraryCache(library)?.kind != Kind.STATIC) {
                val reason = "requires a static cache of ${library.uniqueName}, which this compilation does not use"
                return@mapNotNull ignoreObjCCache(directory, reason)
            }
            val metadata = loadObjCCacheMetadata(archive, library) ?: return@mapNotNull null
            val expected = exportedDependencies(library, exportedLibraries)
            if (metadata.exportedLibraries != expected) {
                val built = metadata.exportedLibraries.joinToString()
                return@mapNotNull ignoreObjCCache(
                        directory,
                        "was built exporting [$built] of the dependencies of ${library.uniqueName}, " +
                                "but this compilation exports [${expected.joinToString()}]",
                )
            }
            val units = loadObjCExportUnits(archive) ?: return@mapNotNull null
            library to ObjCCache(archive.absolutePathString(), metadata, units)
        }.toMap()
    }

    private fun ignoreObjCCache(directory: Path, reason: String): Nothing? {
        configuration.report(CliDiagnostics.KONAN_ARGUMENT_WARNING, "Objective-C cache at $directory $reason. Ignoring cache.")
        return null
    }

    private fun loadObjCCacheMetadata(archive: Path, library: KotlinLibrary): ObjCCacheMetadata? {
        val directory = archive.parent.parent
        return runCatching {
            val properties = Properties()
            archive.resolveSibling(OBJC_CACHE_METADATA_FILE_NAME).bufferedReader().use { properties.load(it) }
            val format = properties.getProperty("format")
            val expectedFormat = ObjCCacheMetadata.FORMAT_VERSION.toString()
            if (format != expectedFormat) {
                val reason = "was built in format ${format ?: "unknown"}, but this compiler reads format $expectedFormat"
                return ignoreObjCCache(directory, reason)
            }
            ObjCCacheMetadata.deserialize(properties, library)
        }.getOrElse { ignoreObjCCache(directory, "is incomplete or unreadable (${it.message})") }
    }

    private fun loadObjCExportUnits(archive: Path): ObjCExportUnits? = runCatching {
        GZIPInputStream(archive.resolveSibling(OBJC_CACHE_UNITS_FILE_NAME).inputStream()).bufferedReader().use { ObjCExportUnits.read(it) }
    }.getOrElse { ignoreObjCCache(archive.parent.parent, "is incomplete or unreadable (${it.message})") }

    private fun findObjCCache(library: KotlinLibrary, moduleName: String, directoriesByName: Map<String, List<Path>>): Path? {
        val candidateName = getObjCCachedLibraryName(library, moduleName)
        for (dir in directoriesByName[candidateName].orEmpty()) {
            val archive = dir.resolve(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)
                    .resolve(getArtifactName(target, candidateName, CompilerOutputKind.OBJC_CACHE))
            if (archive.exists()) return archive.absolute()
            ignoreObjCCache(dir.absolute(), "is incomplete or unreadable (no archive)")
        }
        return null
    }

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
                library to it
            }
        }
    }.toMap()

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

        fun getObjCCachedLibraryName(libraryName: String, moduleName: String): String =
                "$libraryName-$moduleName.objc_cache"

        fun getObjCCachedLibraryName(library: KotlinLibrary, moduleName: String): String =
                getObjCCachedLibraryName(library.uniqueName, moduleName)

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
        const val OBJC_CACHE_METADATA_FILE_NAME = "objc_cache_metadata.properties"
        const val OBJC_CACHE_UNITS_FILE_NAME = "objc_cache_units.txt.gz"
        const val BITCODE_DEPENDENCIES_FILE_NAME = "bitcode_deps"
        const val INLINE_FUNCTION_BODIES_FILE_NAME = "inline_bodies"
        const val CLASS_FIELDS_FILE_NAME = "class_fields"
        const val EAGER_INITIALIZED_PROPERTIES_FILE_NAME = "eager_init"
        const val TRIVIAL_GETTERS_FILE_NAME = "trivial_getters"
        const val OBJC_ADAPTERS_FILE_NAME = "objc_adapters"
        const val PARTIAL_LINKAGE_ISSUES_FILE_NAME = "pl_issues"
    }
}

data class ObjCCacheAdapterEntry(val objcName: String, val isInterface: Boolean, val symbolName: String) {
    companion object {
        fun typeAdapter(objcName: String, isInterface: Boolean) = ObjCCacheAdapterEntry(
            objcName, isInterface, "_Kotlin_ObjCExport_TypeAdapter_${if (isInterface) "Protocol" else "Class"}_$objcName",
        )

        fun categoryAdapter(objcName: String, library: KotlinLibrary) = ObjCCacheAdapterEntry(
            objcName, isInterface = false, "_Kotlin_ObjCExport_CategoryAdapter_${objcName}_${library.uniqueName.replace('.', '_')}",
        )
    }
}

class ObjCCacheMetadata(
    val classAdapters: List<ObjCCacheAdapterEntry>,
    val protocolAdapters: List<ObjCCacheAdapterEntry>,
    val categoryAdapters: List<ObjCCacheAdapterEntry>,
    val exportedLibraries: List<String>,
    val extraClassAdapters: List<ObjCCacheAdapterEntry>,
    val extraProtocolAdapters: List<ObjCCacheAdapterEntry>,
) {
    fun serialize(writer: java.io.Writer) {
        writer.appendLine("format=$FORMAT_VERSION")
        writer.appendLine("classAdapters=${classAdapters.serializeEntries()}")
        writer.appendLine("protocolAdapters=${protocolAdapters.serializeEntries()}")
        writer.appendLine("categoryAdapters=${categoryAdapters.serializeEntries()}")
        writer.appendLine("exportedLibraries=${exportedLibraries.sorted().joinToString(";")}")
        writer.appendLine("extraClassAdapters=${extraClassAdapters.serializeEntries()}")
        writer.appendLine("extraProtocolAdapters=${extraProtocolAdapters.serializeEntries()}")
    }

    companion object {
        const val FORMAT_VERSION = ObjCExportUnits.FORMAT_VERSION

        private fun List<ObjCCacheAdapterEntry>.serializeEntries() = map { it.objcName }.sorted().joinToString(";")

        fun deserialize(properties: Properties, library: KotlinLibrary): ObjCCacheMetadata {
            fun property(key: String): String = properties.getProperty(key) ?: error("Missing '$key' in Objective-C cache metadata")
            fun parseList(key: String): List<String> = property(key).split(";").filter { it.isNotEmpty() }
            fun parseTypeAdapters(key: String, isInterface: Boolean): List<ObjCCacheAdapterEntry> =
                parseList(key).map { ObjCCacheAdapterEntry.typeAdapter(it, isInterface) }
            return ObjCCacheMetadata(
                classAdapters = parseTypeAdapters("classAdapters", isInterface = false),
                protocolAdapters = parseTypeAdapters("protocolAdapters", isInterface = true),
                categoryAdapters = parseList("categoryAdapters").map { ObjCCacheAdapterEntry.categoryAdapter(it, library) },
                exportedLibraries = parseList("exportedLibraries"),
                extraClassAdapters = parseTypeAdapters("extraClassAdapters", isInterface = false),
                extraProtocolAdapters = parseTypeAdapters("extraProtocolAdapters", isInterface = true),
            )
        }
    }
}
