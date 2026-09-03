/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.backend.konan.CacheSupport.Companion.cacheFileId
import org.jetbrains.kotlin.backend.konan.serialization.*
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import java.security.MessageDigest
import org.jetbrains.kotlin.io.canonicalPathString
import org.jetbrains.kotlin.io.listDirectoryEntriesIfDirectoryExists
import org.jetbrains.kotlin.konan.config.filesToCache
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
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

private fun LibraryHashComputer.digestLibrary(library: KotlinLibrary) =
        update(SerializedKlibFingerprint(library.path.toFile()).klibFingerprint)

private fun getArtifactName(target: KonanTarget, baseName: String, kind: CompilerOutputKind) =
        "${kind.prefix(target)}$baseName${kind.suffix(target)}"

class CachedLibraries(
        private val configuration: CompilerConfiguration,
        private val target: KonanTarget,
        allLibraries: List<KotlinLibrary>,
        private val explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<Path>,
        autoCacheDirectory: Path,
        autoCacheableFrom: List<Path>,
        private val libraryToCache: KotlinLibrary?,
        private val compilerFingerprint: String? = null,
) {
    enum class Kind { DYNAMIC, STATIC, HEADER, OBJC }

    sealed class Cache(protected val target: KonanTarget, val kind: Kind, val path: String, val rootDirectory: String) {
        val bitcodeDependencies by lazy { computeBitcodeDependencies() }
        val binariesPaths by lazy { computeBinariesPaths() }
        val serializedInlineFunctionBodies by lazy { computeSerializedInlineFunctionBodies() }
        val serializedClassFields by lazy { computeSerializedClassFields() }
        val serializedEagerInitializedFiles by lazy { computeSerializedEagerInitializedFiles() }
        val serializedTrivialGetters by lazy { computeSerializedTrivialGetters() }

        protected abstract fun computeBitcodeDependencies(): List<DependenciesTracker.UnresolvedDependency>
        protected abstract fun computeBinariesPaths(): List<String>
        protected abstract fun computeSerializedInlineFunctionBodies(): List<SerializedInlineFunctionReference>
        protected abstract fun computeSerializedClassFields(): List<SerializedClassFields>
        protected abstract fun computeSerializedEagerInitializedFiles(): List<SerializedEagerInitializedFile>
        protected abstract fun computeSerializedTrivialGetters(): List<SerializedTrivialGetter>

        protected fun Kind.toCompilerOutputKind(): CompilerOutputKind = when (this) {
            Kind.DYNAMIC -> CompilerOutputKind.DYNAMIC_CACHE
            Kind.STATIC -> CompilerOutputKind.STATIC_CACHE
            Kind.HEADER -> CompilerOutputKind.HEADER_CACHE
            Kind.OBJC -> CompilerOutputKind.OBJC_CACHE
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

            fun getObjCCacheMetadata(): ObjCCacheMetadata? {
                val directory = Path(path).absolute().parent
                val metadataFile = directory.resolve(OBJC_CACHE_METADATA_FILE_NAME)
                if (!metadataFile.exists()) return null
                val metadata = metadataFile.bufferedReader().use { ObjCCacheMetadata.deserialize(it) }
                if (metadata.targetName.isNotEmpty() && metadata.targetName != target.name) {
                    error("Objective-C cache at $path was compiled for target '${metadata.targetName}', but current target is '${target.name}'")
                }
                return metadata
            }

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
        }
    }

    private fun Path.trySelectCacheFor(
        library: KotlinLibrary,
        expectedModuleName: String? = null,
        expectedEntryPointsHash: String? = null,
    ): Cache? {
        // See Linker.renameOutput why is it ok to have an empty cache directory.
        val cacheDirContents = listDirectoryEntriesIfDirectoryExists().map { it.absolutePathString() }.toSet()
        if (cacheDirContents.isEmpty()) return null
        val cacheBinaryPartDir = resolve(PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)
        val cacheBinaryPartDirContents = cacheBinaryPartDir.listDirectoryEntriesIfDirectoryExists().map { it.absolutePathString() }.toSet()
        val baseName = getCachedLibraryName(library)
        val dynamicFile = cacheBinaryPartDir.resolve(getArtifactName(target, baseName, CompilerOutputKind.DYNAMIC_CACHE))
        val staticFile = cacheBinaryPartDir.resolve(getArtifactName(target, baseName, CompilerOutputKind.STATIC_CACHE))
        val headerFile = cacheBinaryPartDir.resolve(getArtifactName(target, baseName, CompilerOutputKind.HEADER_CACHE))

        val objcFile = cacheBinaryPartDir.listDirectoryEntriesIfDirectoryExists().firstOrNull {
            it.name.startsWith(CompilerOutputKind.OBJC_CACHE.prefix(target)) && it.name.endsWith(CompilerOutputKind.OBJC_CACHE.suffix(target))
        }

        if (dynamicFile.absolutePathString() in cacheBinaryPartDirContents && staticFile.absolutePathString() in cacheBinaryPartDirContents)
            error("Both dynamic and static caches files cannot be in the same directory." +
                    " Library: ${library.path}, path to cache: ${absolutePathString()}")
        return when {
            dynamicFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.DYNAMIC, dynamicFile.absolutePathString())
            staticFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.STATIC, staticFile.absolutePathString())
            headerFile.absolutePathString() in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.HEADER, headerFile.absolutePathString())
            objcFile != null && this.name.endsWith(".objc_cache") -> {
                val metadataFile = cacheBinaryPartDir.resolve(OBJC_CACHE_METADATA_FILE_NAME)
                if (metadataFile.exists()) {
                    val metadata = metadataFile.bufferedReader().use { ObjCCacheMetadata.deserialize(it) }
                    if (metadata.targetName.isNotEmpty() && metadata.targetName != target.name) {
                        configuration.report(
                            CliDiagnostics.KONAN_ARGUMENT_WARNING,
                            "Objective-C cache at ${absolutePathString()} was compiled for target '${metadata.targetName}', but current target is '${target.name}'. Ignoring cache."
                        )
                        return null
                    }
                    if (metadata.compilerFingerprint.isNotEmpty() && compilerFingerprint != null && metadata.compilerFingerprint != compilerFingerprint) {
                        configuration.report(
                            CliDiagnostics.KONAN_ARGUMENT_WARNING,
                            "Objective-C cache at ${absolutePathString()} was compiled with different compiler (fingerprint '${metadata.compilerFingerprint}'), but current compiler fingerprint is '$compilerFingerprint'. Ignoring cache."
                        )
                        return null
                    }
                    if (metadata.klibHash.isNotEmpty()) {
                        val currentKlibHash = SerializedKlibFingerprint(library.path.toFile()).klibFingerprint.toString()
                        if (metadata.klibHash != currentKlibHash) {
                            configuration.report(
                                CliDiagnostics.KONAN_ARGUMENT_WARNING,
                                "Objective-C cache at ${absolutePathString()} was compiled for library '${library.path}' with hash '${metadata.klibHash}', but current library hash is '$currentKlibHash'. Ignoring cache."
                            )
                            return null
                        }
                    }
                    if (!expectedModuleName.isNullOrEmpty() && metadata.moduleName.isNotEmpty() && metadata.moduleName != expectedModuleName) {
                        configuration.report(
                            CliDiagnostics.KONAN_ARGUMENT_WARNING,
                            "Objective-C cache at ${absolutePathString()} was compiled for module '${metadata.moduleName}', but current module is '$expectedModuleName'. Ignoring cache."
                        )
                        return null
                    }
                    if (!expectedEntryPointsHash.isNullOrEmpty() && metadata.entryPointsHash.isNotEmpty() && metadata.entryPointsHash != expectedEntryPointsHash) {
                        configuration.report(
                            CliDiagnostics.KONAN_ARGUMENT_WARNING,
                            "Objective-C cache at ${absolutePathString()} was compiled with different entry points (hash '${metadata.entryPointsHash}'), but current entry points hash is '$expectedEntryPointsHash'. Ignoring cache."
                        )
                        return null
                    }
                }
                Cache.Monolithic(target, Kind.OBJC, objcFile.absolutePathString())
            }
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

    private val uniqueNameToLibrary = allLibraries.associateBy { it.uniqueName }
    private val uniqueNameToHash = mutableMapOf<String, FingerprintHash>()

    private val cacheNameToImplicitDirsMapping: Map<String, List<Path>> =
            implicitCacheDirectories.flatMap { dir -> dir.listDirectoryEntriesIfDirectoryExists() }
                    .groupBy { it.name }

    fun getObjCCache(library: KotlinLibrary, moduleName: String): Cache? {
        val candidateName = if (moduleName.isNotEmpty()) getObjCCachedLibraryName(library, moduleName)
        else "${library.uniqueName}.objc_cache"

        val expectedEntryPointsHash = ObjCCacheMetadata.computeEntryPointsHash(configuration)

        val explicitPath = explicitCaches[library]
            ?: explicitCaches.entries.firstOrNull { it.key.uniqueName == library.uniqueName }?.value
        if (explicitPath != null) {
            val explicitDir = Path(explicitPath)
            val explicitCache = explicitDir.trySelectCacheFor(library, moduleName, expectedEntryPointsHash)?.takeIf { it.kind == Kind.OBJC }
                ?: explicitDir.resolve(candidateName).trySelectCacheFor(library, moduleName, expectedEntryPointsHash)?.takeIf { it.kind == Kind.OBJC }
            if (explicitCache != null) return explicitCache
        }

        val dirs = cacheNameToImplicitDirsMapping[candidateName].orEmpty()
        for (dir in dirs) {
            val cache = dir.trySelectCacheFor(library, moduleName, expectedEntryPointsHash)
            if (cache != null && cache.kind == Kind.OBJC) return cache
        }
        return null
    }

    private fun KotlinLibrary.trySelectCacheAt(dirBuilder: (String) -> List<Path>) =
            sequenceOf(getPerFileCachedLibraryName(this), getCachedLibraryName(this))
                    .flatMap(dirBuilder)
                    .mapNotNull { it.trySelectCacheFor(this) }
                    .filter { it.kind != Kind.OBJC }
                    .firstOrNull()

    private val allCaches: Map<KotlinLibrary, Cache> = allLibraries.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            val candidateCache = Path(explicitPath).trySelectCacheFor(library)
                    ?: error("No cache found for library ${library.path} at $explicitPath")
            if (candidateCache.kind == Kind.OBJC) null else candidateCache
        } else {
            val libraryPath = library.path.canonicalPathString()
            library.trySelectCacheAt { cacheNameToImplicitDirsMapping[it].orEmpty() }
                    ?: autoCacheDirectory.takeIf { autoCacheableFrom.any { libraryPath.startsWith(it.canonicalPathString()) } }
                            ?.let {
                                val dir = computeLibraryCacheDirectory(it, library, uniqueNameToLibrary, uniqueNameToHash)
                                library.trySelectCacheAt { cacheName -> listOfNotNull(dir.resolve(cacheName)) }
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

        fun getObjCCachedLibraryName(libraryName: String, moduleName: String): String = "$libraryName-$moduleName.objc_cache"
        fun getObjCCachedLibraryName(library: KotlinLibrary, moduleName: String): String = getObjCCachedLibraryName(library.uniqueName, moduleName)

        private fun computeLibraryHash(library: KotlinLibrary, librariesHashes: MutableMap<String, FingerprintHash>) =
                librariesHashes.getOrPut(library.uniqueName) {
                    val hashComputer = LibraryHashComputer()
                    hashComputer.digestLibrary(library)
                    hashComputer.digest()
                }

        fun computeDependenciesFingerprint(
                dependencies: List<KotlinLibrary>,
                librariesHashes: MutableMap<String, FingerprintHash>,
        ): FingerprintHash {
            val hashComputer = LibraryHashComputer()
            dependencies.sortedBy { it.uniqueName }.forEach {
                hashComputer.update(computeLibraryHash(it, librariesHashes))
            }
            return hashComputer.digest()
        }

        fun computeLibraryCacheDirectory(
                baseCacheDirectory: Path,
                library: KotlinLibrary,
                allLibraries: Map<String, KotlinLibrary>,
                librariesHashes: MutableMap<String, FingerprintHash>,
        ): Path {
            val dependencies = library.getAllTransitiveDependencies(allLibraries)
            val fingerprintHash = computeDependenciesFingerprint(listOf(library) + dependencies, librariesHashes)
            return baseCacheDirectory.resolve(library.uniqueName).resolve(fingerprintHash.toString())
        }

        const val PER_FILE_CACHE_IR_LEVEL_DIR_NAME = "ir"
        const val PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME = "bin"

        const val METADATA_FILE_NAME = "metadata.properties"
        const val OBJC_CACHE_METADATA_FILE_NAME = "objc_cache_metadata.properties"
        const val BITCODE_DEPENDENCIES_FILE_NAME = "bitcode_deps"
        const val INLINE_FUNCTION_BODIES_FILE_NAME = "inline_bodies"
        const val CLASS_FIELDS_FILE_NAME = "class_fields"
        const val EAGER_INITIALIZED_PROPERTIES_FILE_NAME = "eager_init"
        const val TRIVIAL_GETTERS_FILE_NAME = "trivial_getters"
    }
}

data class ObjCCacheAdapterEntry(val objcName: String, val isInterface: Boolean, val symbolName: String)

class ObjCCacheMetadata(
    val targetName: String = "",
    val klibHash: String = "",
    val compilerFingerprint: String = "",
    val moduleName: String = "",
    val entryPointsHash: String = "",
    val classAdapters: List<ObjCCacheAdapterEntry>,
    val protocolAdapters: List<ObjCCacheAdapterEntry>,
) {
    fun serialize(writer: java.io.Writer) {
        val properties = java.util.Properties()
        if (targetName.isNotEmpty()) {
            properties.setProperty("targetName", targetName)
        }
        if (klibHash.isNotEmpty()) {
            properties.setProperty("klibHash", klibHash)
        }
        if (compilerFingerprint.isNotEmpty()) {
            properties.setProperty("compilerFingerprint", compilerFingerprint)
        }
        if (moduleName.isNotEmpty()) {
            properties.setProperty("moduleName", moduleName)
        }
        if (entryPointsHash.isNotEmpty()) {
            properties.setProperty("entryPointsHash", entryPointsHash)
        }
        val classAdaptersStr = classAdapters.joinToString(";") { "${it.objcName},${it.symbolName}" }
        val protocolAdaptersStr = protocolAdapters.joinToString(";") { "${it.objcName},${it.symbolName}" }
        properties.setProperty("classAdapters", classAdaptersStr)
        properties.setProperty("protocolAdapters", protocolAdaptersStr)
        properties.store(writer, null)
    }

    companion object {
        fun computeEntryPointsHash(configuration: CompilerConfiguration): String {
            val pathStr = configuration.get(BinaryOptions.objcExportEntryPointsPath) ?: return "ALL"
            val path = Path(pathStr)
            if (!path.exists()) return "ALL"
            val bytes = runCatching { path.readBytes() }.getOrNull() ?: return "ALL"
            val expand = configuration.getBoolean(BinaryOptions.objcExportExpandEntryPoints)
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes)
            digest.update(if (expand) 1.toByte() else 0.toByte())
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun deserialize(reader: java.io.Reader): ObjCCacheMetadata {
            val properties = java.util.Properties()
            properties.load(reader)
            val targetName = properties.getProperty("targetName").orEmpty()
            val klibHash = properties.getProperty("klibHash").orEmpty()
            val compilerFingerprint = properties.getProperty("compilerFingerprint").orEmpty()
            val moduleName = properties.getProperty("moduleName").orEmpty()
            val entryPointsHash = properties.getProperty("entryPointsHash").orEmpty()
            fun parseEntries(value: String?, isInterface: Boolean): List<ObjCCacheAdapterEntry> {
                if (value.isNullOrEmpty()) return emptyList()
                return value.split(";").filter { it.isNotEmpty() }.mapNotNull {
                    val parts = it.split(",")
                    if (parts.size >= 2) ObjCCacheAdapterEntry(parts[0], isInterface, parts[1]) else null
                }
            }
            return ObjCCacheMetadata(
                targetName = targetName,
                klibHash = klibHash,
                compilerFingerprint = compilerFingerprint,
                moduleName = moduleName,
                entryPointsHash = entryPointsHash,
                classAdapters = parseEntries(properties.getProperty("classAdapters"), isInterface = false),
                protocolAdapters = parseEntries(properties.getProperty("protocolAdapters"), isInterface = true),
            )
        }
    }
}
