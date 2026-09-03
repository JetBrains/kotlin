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
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.konan.config.filesToCache
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.javaFile
import org.jetbrains.kotlin.library.isNativeStdlib
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
        private val configuration: CompilerConfiguration,
        private val target: KonanTarget,
        allLibraries: List<KotlinLibrary>,
        private val explicitCaches: Map<KotlinLibrary, String>,
        implicitCacheDirectories: List<File>,
        autoCacheDirectory: File,
        autoCacheableFrom: List<File>,
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

        class Monolithic(target: KonanTarget, kind: Kind, path: String)
            : Cache(target, kind, path, File(path).parentFile.parentFile.absolutePath)
        {
            override fun computeBitcodeDependencies(): List<DependenciesTracker.UnresolvedDependency> {
                val directory = File(path).absoluteFile.parentFile
                val data = directory.child(BITCODE_DEPENDENCIES_FILE_NAME).readStrings()
                return DependenciesSerializer.deserialize(path, data)
            }

            override fun computeBinariesPaths() = listOf(path)

            fun getObjCCacheMetadata(): ObjCCacheMetadata? {
                val directory = File(path).absoluteFile.parentFile
                val metadataFile = directory.child(OBJC_CACHE_METADATA_FILE_NAME)
                if (!metadataFile.exists) return null
                val metadata = metadataFile.bufferedReader().use { ObjCCacheMetadata.deserialize(it) }
                if (metadata.targetName.isNotEmpty() && metadata.targetName != target.name) {
                    error("Objective-C cache at $path was compiled for target '${metadata.targetName}', but current target is '${target.name}'")
                }
                return metadata
            }

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

            override fun computeSerializedTrivialGetters() = mutableListOf<SerializedTrivialGetter>().also {
                val directory = File(path).absoluteFile.parentFile.parentFile
                val data = directory.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(TRIVIAL_GETTERS_FILE_NAME).readBytes()
                TrivialGettersSerializer.deserializeTo(data, it)
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

            fun getMetadata(file: String) = File(path).child(file).child(METADATA_FILE_NAME).bufferedReader().use {
                CacheMetadataSerializer.deserialize(it)
            }

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

            override fun computeSerializedTrivialGetters() = mutableListOf<SerializedTrivialGetter>().also {
                existingFileDirs.forEach { fileDir ->
                    val data = fileDir.child(PER_FILE_CACHE_IR_LEVEL_DIR_NAME).child(TRIVIAL_GETTERS_FILE_NAME).readBytes()
                    TrivialGettersSerializer.deserializeTo(data, it)
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
        val headerFile = cacheBinaryPartDir.child(getArtifactName(target, baseName, CompilerOutputKind.HEADER_CACHE))

        val objcFile = cacheBinaryPartDir.listFilesOrEmpty.firstOrNull {
            it.name.startsWith(CompilerOutputKind.OBJC_CACHE.prefix(target)) && it.name.endsWith(CompilerOutputKind.OBJC_CACHE.suffix(target))
        }

        if (dynamicFile.absolutePath in cacheBinaryPartDirContents && staticFile.absolutePath in cacheBinaryPartDirContents)
            error("Both dynamic and static caches files cannot be in the same directory." +
                    " Library: ${library.location}, path to cache: $absolutePath")
        return when {
            dynamicFile.absolutePath in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.DYNAMIC, dynamicFile.absolutePath)
            staticFile.absolutePath in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.STATIC, staticFile.absolutePath)
            headerFile.absolutePath in cacheBinaryPartDirContents -> Cache.Monolithic(target, Kind.HEADER, headerFile.absolutePath)
            objcFile != null && this.name.endsWith(".objc_cache") -> {
                val metadataFile = cacheBinaryPartDir.child(OBJC_CACHE_METADATA_FILE_NAME)
                if (metadataFile.exists) {
                    val metadata = metadataFile.bufferedReader().use { ObjCCacheMetadata.deserialize(it) }
                    if (metadata.targetName.isNotEmpty() && metadata.targetName != target.name) {
                        error("Objective-C cache at $absolutePath was compiled for target '${metadata.targetName}', but current target is '${target.name}'")
                    }
                    if (metadata.compilerFingerprint.isNotEmpty() && compilerFingerprint != null && metadata.compilerFingerprint != compilerFingerprint) {
                        error("Objective-C cache at $absolutePath was compiled with different compiler (fingerprint '${metadata.compilerFingerprint}'), but current compiler fingerprint is '$compilerFingerprint'")
                    }
                    if (metadata.klibHash.isNotEmpty()) {
                        val currentKlibHash = SerializedKlibFingerprint(library.libraryFile.javaFile()).klibFingerprint.toString()
                        if (metadata.klibHash != currentKlibHash) {
                            error("Objective-C cache at $absolutePath was compiled for library '${library.location}' with hash '${metadata.klibHash}', but current library hash is '$currentKlibHash'")
                        }
                    }
                }
                Cache.Monolithic(target, Kind.OBJC, objcFile.absolutePath)
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
                    child(cacheFileId(fqName, filePath))
                }
                Cache.PerFile(target, Kind.STATIC, absolutePath,
                        libraryFileDirs.filterNot { it.name in fileIdsToCache },
                        complete = cacheDirContents.containsAll(libraryFileDirs.map { it.absolutePath }))
            }
        }
    }

    private val uniqueNameToLibrary = allLibraries.associateBy { it.uniqueName }
    private val uniqueNameToHash = mutableMapOf<String, FingerprintHash>()

    private val cacheNameToImplicitDirsMapping: Map<String, List<File>> =
            implicitCacheDirectories.flatMap { dir -> dir.listFilesOrEmpty }
                    .groupBy { it.name }

    fun getObjCCache(library: KotlinLibrary, moduleName: String): Cache? {
        val candidateNames = listOf(
            if (moduleName.isNotEmpty()) getObjCCachedLibraryName(library, moduleName)
            else "${library.uniqueName}.objc_cache"
        )

        val explicitPath = explicitCaches[library]
            ?: explicitCaches.entries.firstOrNull { it.key.uniqueName == library.uniqueName }?.value
        if (explicitPath != null) {
            val explicitDir = File(explicitPath)
            val explicitCache = explicitDir.trySelectCacheFor(library)?.takeIf { it.kind == Kind.OBJC }
                ?: candidateNames.firstNotNullOfOrNull { name ->
                    explicitDir.child(name).trySelectCacheFor(library)?.takeIf { it.kind == Kind.OBJC }
                }
            if (explicitCache != null) return explicitCache
        }

        for (name in candidateNames) {
            val dirs = cacheNameToImplicitDirsMapping[name].orEmpty()
            for (dir in dirs) {
                val cache = dir.trySelectCacheFor(library)
                if (cache != null && cache.kind == Kind.OBJC) return cache
            }
        }
        return null
    }

    private fun KotlinLibrary.trySelectCacheAt(dirBuilder: (String) -> List<File>) =
            sequenceOf(getPerFileCachedLibraryName(this), getCachedLibraryName(this))
                    .flatMap(dirBuilder)
                    .mapNotNull { it.trySelectCacheFor(this) }
                    .filter { it.kind != Kind.OBJC }
                    .firstOrNull()

    private val allCaches: Map<KotlinLibrary, Cache> = allLibraries.mapNotNull { library ->
        val explicitPath = explicitCaches[library]

        val cache = if (explicitPath != null) {
            val candidateCache = File(explicitPath).trySelectCacheFor(library)
                    ?: error("No cache found for library ${library.location} at $explicitPath")
            if (candidateCache.kind == Kind.OBJC) null else candidateCache
        } else {
            val libraryPath = library.libraryFile.canonicalPath
            library.trySelectCacheAt { cacheNameToImplicitDirsMapping[it].orEmpty() }
                    ?: autoCacheDirectory.takeIf { autoCacheableFrom.any { libraryPath.startsWith(it.canonicalPath) } }
                            ?.let {
                                val dir = computeLibraryCacheDirectory(it, library, uniqueNameToLibrary, uniqueNameToHash)
                                library.trySelectCacheAt { cacheName -> listOfNotNull(dir.child(cacheName)) }
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

        fun computeLibraryCacheDirectory(
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

            val hashString = hashComputer.digest().toString()
            return baseCacheDirectory.child(library.uniqueName).child(hashString)
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
    val classAdapters: List<ObjCCacheAdapterEntry>,
    val protocolAdapters: List<ObjCCacheAdapterEntry>,
) {
    @Deprecated("For binary compatibility with older cache artifacts", level = DeprecationLevel.HIDDEN)
    constructor(targetName: String, classAdapters: List<ObjCCacheAdapterEntry>, protocolAdapters: List<ObjCCacheAdapterEntry>) :
        this(targetName = targetName, klibHash = "", compilerFingerprint = "", classAdapters = classAdapters, protocolAdapters = protocolAdapters)

    @Deprecated("For binary compatibility with older cache artifacts", level = DeprecationLevel.HIDDEN)
    constructor(classAdapters: List<ObjCCacheAdapterEntry>, protocolAdapters: List<ObjCCacheAdapterEntry>) :
        this(targetName = "", klibHash = "", compilerFingerprint = "", classAdapters = classAdapters, protocolAdapters = protocolAdapters)

    fun serialize(writer: java.io.Writer) {
        if (targetName.isNotEmpty()) {
            writer.write("targetName=$targetName\n")
        }
        if (klibHash.isNotEmpty()) {
            writer.write("klibHash=$klibHash\n")
        }
        if (compilerFingerprint.isNotEmpty()) {
            writer.write("compilerFingerprint=$compilerFingerprint\n")
        }
        val classAdaptersStr = classAdapters.joinToString(";") { "${it.objcName},${it.symbolName}" }
        val protocolAdaptersStr = protocolAdapters.joinToString(";") { "${it.objcName},${it.symbolName}" }
        writer.write("classAdapters=$classAdaptersStr\n")
        writer.write("protocolAdapters=$protocolAdaptersStr\n")
        writer.flush()
    }

    companion object {
        fun deserialize(reader: java.io.Reader): ObjCCacheMetadata {
            val properties = java.util.Properties()
            properties.load(reader)
            val targetName = properties.getProperty("targetName").orEmpty()
            val klibHash = properties.getProperty("klibHash").orEmpty()
            val compilerFingerprint = properties.getProperty("compilerFingerprint").orEmpty()
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
                classAdapters = parseEntries(properties.getProperty("classAdapters"), isInterface = false),
                protocolAdapters = parseEntries(properties.getProperty("protocolAdapters"), isInterface = true),
            )
        }

        fun readObjCCacheMetadata(reader: java.io.Reader): ObjCCacheMetadata = deserialize(reader)

        fun readObjCCacheMetadata(file: File): ObjCCacheMetadata? {
            if (!file.exists) return null
            return file.bufferedReader().use { deserialize(it) }
        }
    }
}
