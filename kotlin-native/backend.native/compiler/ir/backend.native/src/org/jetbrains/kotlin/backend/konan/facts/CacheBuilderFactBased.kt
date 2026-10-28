/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.facts

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.backend.konan.CacheBuilder
import org.jetbrains.kotlin.backend.konan.CacheSupport
import org.jetbrains.kotlin.backend.konan.CachedLibraries
import org.jetbrains.kotlin.backend.konan.CompilationSpawner
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.backend.konan.TestRunnerKind
import org.jetbrains.kotlin.backend.konan.facts.CacheBuilderFactBased.Companion.CACHE_MANIFEST_FILE_NAME
import org.jetbrains.kotlin.backend.konan.facts.CacheBuilderFactBased.Companion.CACHE_MANIFEST_HUMAN_READABLE_FILE_NAME
import org.jetbrains.kotlin.backend.konan.getFilesWithFqNames
import org.jetbrains.kotlin.backend.konan.isFinalBinary
import org.jetbrains.kotlin.backend.konan.isFullCache
import org.jetbrains.kotlin.backend.konan.setupCommonOptionsForCaches
import org.jetbrains.kotlin.backend.konan.util.reportCompilationErrorAndThrow
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.io.canonicalPathString
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.library.isExplicitlySpecifiedByUserInCLIArgument
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.pathString

internal class CacheManifest(val defs: DefsAssociation, val uses: UsesAssociation) {
    fun withoutFiles(files: Set<FileName>) = CacheManifest(defs.filterKeys { it !in files }, uses.filterKeys { it !in files })
}

internal class LibraryCacheLayout(val root: Path) {
    fun bin(fileId: String): Path = root.resolve(fileId).resolve(CachedLibraries.PER_FILE_CACHE_BINARY_LEVEL_DIR_NAME)
    val manifest: Path = root.resolve(CACHE_MANIFEST_FILE_NAME)
    val humanReadableManifest: Path = root.resolve(CACHE_MANIFEST_HUMAN_READABLE_FILE_NAME)

    fun readManifest(): CacheManifest? {
        if (!manifest.exists()) return null
        val [defs, uses] = BinaryFactsDeserializer.deserializeFrom(manifest)
        return CacheManifest(defs, uses)
    }

    fun writeManifest(cacheManifest: CacheManifest) {
        BinaryFactsSerializer.serializeTo(manifest, cacheManifest.defs, cacheManifest.uses)
        DebugFactsSerializer.serializeTo(humanReadableManifest, cacheManifest.defs, cacheManifest.uses)
    }
}

/**
 * A toy version of [CacheBuilder] that stores, next to the compiled artifacts of each library, the facts
 * its files define and use (see [FactsTracker]), so that the next compilation can read them back.
 *
 * Enabled with `-Xfacts-cache-enabled`, it only handles the user libraries (see [isSubjectOfFactsCache]):
 * the stdlib (with the Native runtime), the cinterop and the external libraries are still cached by [CacheBuilder],
 * which has to run first.
 *
 * Each library has its own per-file cache folder (same location and name as with [CacheBuilder]):
 * ```
 * <cache directory>/<library unique name>-per-file-cache/
 *     <file id>/bin/        the compiled artifacts of a file (see [CacheSupport.cacheFileId])
 *     cache_manifest        the facts of all the files (see [BinaryFactsSerializer])
 *     cache_manifest.csv    the same facts, human-readable (see [DebugFactsSerializer])
 * ```
 *
 * A file is rebuilt when it is new, when its IR has changed, or when it used a fact whose hash has changed
 * (or which is not defined anymore) in one of the libraries handled here.
 */
class CacheBuilderFactBased(
        val config: NativeSecondStageCompilationConfig,
        val compilationSpawner: CompilationSpawner
) {

    private val KotlinLibrary.isExternal: Boolean
        get() {
            val libraryCanonicalPath = Path(path.canonicalPathString())
            return autoCacheableFrom.any { libraryCanonicalPath.startsWith(it) }
        }

    private val KotlinLibrary.isSubjectOfIC: Boolean
        get() = isExplicitlySpecifiedByUserInCLIArgument && !isExternal && !isNativeStdlib

    private val KotlinLibrary.isSubjectOfFactsCache: Boolean
        get() = isSubjectOfIC && !isCInteropLibrary()

    private val configuration = config.configuration
    private val autoCacheableFrom = configuration[NativeConfigurationKeys.AUTO_CACHEABLE_FROM]!!.map { Path(Path(it).canonicalPathString()) }
    private val icEnabled = configuration[CommonConfigurationKeys.INCREMENTAL_COMPILATION]!!
    private val factsCacheEnabled = configuration.factsCacheEnabled
    private val includedLibraries = configuration.konanIncludedLibraries.map { Path(it).canonicalPathString() }.toSet()
    private val generateTestRunner = configuration.getNotNull(NativeConfigurationKeys.GENERATE_TEST_RUNNER)

    private val cacheRootDirectories = mutableMapOf<KotlinLibrary, String>()

    /** The IDs of the files rebuilt during this compilation, or `null` if the whole library has been rebuilt. */
    private val rebuiltFileIds = mutableMapOf<KotlinLibrary, Set<String>?>()

    /** Whether the cache of [fileId] of [library] has been built during this compilation, see [CacheBuilder.wasRebuiltInThisRun]. */
    fun wasRebuiltInThisRun(library: KotlinLibrary, fileId: String?): Boolean {
        if (library !in rebuiltFileIds) return false
        val fileIds = rebuiltFileIds[library] ?: return true
        return fileId in fileIds
    }

    /** The libraries rebuilt from scratch, without previous facts to compare against: their dependents are rebuilt from scratch too. */
    private val librariesRebuiltWithoutFacts = mutableSetOf<KotlinLibrary>()

    /** The facts whose hash has changed (or that are not defined anymore) in the libraries processed so far. */
    private val changedFacts = mutableSetOf<FactKey>()

    /** The facts recorded by the previous compilation, for the libraries whose cache has a manifest. */
    private val previousManifests = mutableMapOf<KotlinLibrary, CacheManifest>()

    internal fun previousManifest(library: KotlinLibrary): CacheManifest? = previousManifests[library]

    val needToBuild: Boolean
        get() = factsCacheEnabled
                && icEnabled
                && config.ignoreCacheReason == null
                && (config.isFinalBinary || config.produce.isFullCache)

    @OptIn(ExperimentalPathApi::class)
    fun build() {
        // Note: The libraries should be in the reverse topo-order here!
        for (library in config.klibDag.librariesReverseTopoSorted) {
            // For MinGW target avoid compiling caches for anything except stdlib.
            if (config.target == KonanTarget.MINGW_X64 && !library.isNativeStdlib) continue

            val isSubjectOfFactsCache = library.isSubjectOfFactsCache
            val cache = config.cachedLibraries.getLibraryCache(library, allowIncomplete = library.isSubjectOfIC)
            cache?.let { cacheRootDirectories[library] = it.rootDirectory }
            // The other libraries are cached by [CacheBuilder].
            if (!isSubjectOfFactsCache) continue

            val previousManifest = (cache as? CachedLibraries.Cache.PerFile)?.let {
                timed("manifest-read", library) { LibraryCacheLayout(Path(it.rootDirectory)).readManifest() }
            }
            val dependencyRebuiltWithoutFacts = config.klibDag.getAllDependencies(library).any { it in librariesRebuiltWithoutFacts }

            if (cache is CachedLibraries.Cache.PerFile && previousManifest != null && !dependencyRebuiltWithoutFacts) {
                previousManifests[library] = previousManifest
                updateLibraryCache(library, cache, previousManifest)
            } else {
                // A cache without a manifest (e.g. a monolithic one, or one produced by [CacheBuilder]) can't be updated here.
                cache?.let {
                    Path(it.rootDirectory).deleteRecursively()
                    cacheRootDirectories.remove(library)
                }
                librariesRebuiltWithoutFacts += library
                buildLibraryCache(library, filesToCache = null, previousManifest = null)
            }
        }

        configuration.reportLog("FACTS FROM THE PREVIOUS COMPILATION")
        previousManifests.forEach { [library, manifest] ->
            configuration.reportLog("    ${library.uniqueName}: ${manifest.defs.size} files with defined facts, ${manifest.uses.size} files with used facts")
        }
    }

    /**
     * Rebuilds the files of [library] that are dirty with respect to [previousManifest], and records the facts
     * whose hash has changed for the libraries depending on [library].
     */
    @OptIn(ExperimentalPathApi::class)
    private fun updateLibraryCache(library: KotlinLibrary, cache: CachedLibraries.Cache.PerFile, previousManifest: CacheManifest) {
        configuration.reportLog("FACTS-BASED IC ANALYSIS of ${library.path}")

        // Cheap check first: which files are added, changed (IR fingerprint) or removed, without deserializing the IR.
        val fingerprintCheckStart = System.nanoTime()
        val cacheRoot = Path(cache.rootDirectory)
        val cachedFileIds = cacheRoot.listDirectoryEntries().filter { it.isDirectory() }.map { it.name }.toSet()
        val actualFiles = library.getFilesWithFqNames()
        val actualFileIds = actualFiles.map { CacheSupport.cacheFileId(it.fqName, it.filePath) }
        val modificationReasons = actualFiles.indices.map { index ->
            val fileId = actualFileIds[index]
            when {
                fileId !in cachedFileIds -> "added"
                cache.getMetadataOrNull(fileId)?.hash != SerializedIrFileFingerprint(library, index).fileFingerprint -> "changed"
                else -> null
            }
        }
        val removedFileIds = cachedFileIds - actualFileIds.toSet()
        val removedFiles = (previousManifest.defs.keys + previousManifest.uses.keys) - actualFiles.map { it.filePath }.toSet()
        reportTiming("fingerprint-check", library, fingerprintCheckStart)

        // The facts defined by a library depend only on its own IR (and on the bridges policy, fixed for a cache):
        // if no file has been added, changed or removed, none of them has changed and there's no need to generate them.
        // TODO: do the same per file, generating the facts only of the added / changed files.
        val isLibraryModified = modificationReasons.any { it != null } || removedFileIds.isNotEmpty() || removedFiles.isNotEmpty()
        if (isLibraryModified) {
            val currentHashes = timed("facts-generation", library) {
                val threadsCount = config.threadsCount
                KlibFactsGeneratorImpl(threadsCount, config.bridgesPolicy).generateFactsFrom(library).definedFacts.values
                        .flatMap { it.entries }
                        .associate { it.key to it.value }
            }
            previousManifest.defs.values.forEach { defs ->
                defs.forEach { [factKey, hash] -> if (currentHashes[factKey] != hash) changedFacts += factKey }
            }
        } else {
            configuration.reportLog("    SKIPPED FACTS GENERATION (unchanged klib)")
        }

        val analysisStart = System.nanoTime()
        val dirtyFiles = actualFiles.withIndex().mapNotNull { [index, file] ->
            val reason = modificationReasons[index]
                    ?: "uses changed facts".takeIf { previousManifest.uses[file.filePath].orEmpty().keys.any { it in changedFacts } }
            reason?.let {
                configuration.reportLog("    DIRTY ($it): ${file.filePath}")
                file.filePath
            }
        }
        reportTiming("dirty-analysis", library, analysisStart)

        removedFileIds.forEach {
            configuration.reportLog("    REMOVED: $it")
            cacheRoot.resolve(it).deleteRecursively()
        }
        val manifest = previousManifest.withoutFiles(removedFiles)

        if (dirtyFiles.isEmpty()) {
            configuration.reportLog("    UP-TO-DATE")
            if (removedFiles.isNotEmpty()) LibraryCacheLayout(cacheRoot).writeManifest(manifest)
            return
        }
        buildLibraryCache(library, dirtyFiles, manifest)
    }

    /**
     * Builds the per-file cache of [filesToCache] of [library] (of all its files if `null`)
     * and merges the facts they define and use into [previousManifest].
     */
    private fun buildLibraryCache(library: KotlinLibrary, filesToCache: List<String>?, previousManifest: CacheManifest?) {
        val dependencies = config.klibDag.getAllDependencies(library).toList()
        val dependencyCaches = dependencies.map {
            cacheRootDirectories[it] ?: run {
                configuration.reportLog("SKIPPING ${library.path} as some of the dependencies aren't cached")
                return
            }
        }

        val libraryCacheDirectory = config.incrementalCacheDirectory!!
        val layout = LibraryCacheLayout(libraryCacheDirectory.resolve(CachedLibraries.getPerFileCachedLibraryName(library)))
        libraryCacheDirectory.createDirectories()

        configuration.reportLog("CACHING ${library.path} to ${layout.root.absolutePathString()}")
        filesToCache?.forEach { configuration.reportLog("    $it") }

        spawnCacheBuild(library, dependencies, dependencyCaches, libraryCacheDirectory, filesToCache, previousManifest, layout)

        cacheRootDirectories[library] = layout.root.absolutePathString()
        rebuiltFileIds[library] = filesToCache?.let { files ->
            library.getFilesWithFqNames().filter { it.filePath in files }.map { CacheSupport.cacheFileId(it.fqName, it.filePath) }.toSet()
        }
    }

    /**
     * Spawns the compilation of the per-file cache of [library] into [layout] and writes the facts it tracked,
     * merged into [previousManifest] for an incremental build, to the cache manifest.
     */
    @OptIn(ExperimentalPathApi::class)
    private fun spawnCacheBuild(
            library: KotlinLibrary,
            dependencies: List<KotlinLibrary>,
            dependencyCaches: List<String>,
            cacheDirectory: Path,
            filesToCache: List<String>?,
            previousManifest: CacheManifest?,
            layout: LibraryCacheLayout,
    ) {
        // The files of the cache folder are atomically renamed from temp ones by the spawned compilation, so dump the facts aside.
        val factsDump = Files.createTempFile(cacheDirectory, layout.root.name, ".facts.csv")
        try {
            val buildStart = System.nanoTime()
            compilationSpawner.spawn(config.additionalCacheFlags) {
                config.configuration.konanHome?.let { konanHome = it }
                setupCommonOptionsForCaches(config)
                konanProducedArtifactKind = CompilerOutputKind.STATIC_CACHE
                checkDependencies = true
                konanNoDefaultLibs = true
                konanNoStdlib = true
                konanLibraries = (dependencies + library).map { it.canonicalPath.pathString }
                cachedLibraries = dependencies.zip(dependencyCaches).associate { [dependency, dependencyCache] ->
                    dependency.canonicalPath.pathString to dependencyCache
                }
                cacheDirectories = listOf(cacheDirectory.absolutePathString())
                makePerFileCache = true
                filesToCache?.let { this.filesToCache = it }
                serializedKlibDag = config.klibDag.serialize()
                val libraryPath = library.canonicalPath.pathString
                konanLibraryToAddToCache = libraryPath
                // Same as [CacheBuilder]: the test runner of an included library is generated when building its cache.
                val generateTestRunner = this@CacheBuilderFactBased.generateTestRunner
                if (generateTestRunner != TestRunnerKind.NONE && libraryPath in includedLibraries) {
                    konanFriendLibraries = config.loadedKlibs.friends.map { it.canonicalPath.pathString }
                    this.generateTestRunner = generateTestRunner
                    konanIncludedLibraries = listOf(libraryPath)
                    configuration.testDumpOutputPath?.let { testDumpOutputPath = it }
                }
                dumpFactsTo = factsDump.absolutePathString()
            }
            reportTiming("cache-build", library, buildStart)

            timed("manifest-write", library) {
                val [defs, uses] = DebugFactsDeserializer.deserializeFrom(factsDump)
                val manifest = if (filesToCache == null) {
                    CacheManifest(defs, uses)
                } else {
                    // Only the facts of the rebuilt files are up to date in the dump, the other ones are kept from the previous compilation.
                    val rebuiltFiles = filesToCache.toSet()
                    val previous = previousManifest?.withoutFiles(rebuiltFiles)
                    CacheManifest(
                            previous?.defs.orEmpty() + defs.filterKeys { it in rebuiltFiles },
                            previous?.uses.orEmpty() + uses.filterKeys { it in rebuiltFiles },
                    )
                }
                layout.writeManifest(manifest)
            }
        } catch (t: Throwable) {
            try {
                layout.root.deleteRecursively()
            } catch (_: Throwable) {
                // Nothing to do.
            }
            val message = (t as? CompilationErrorException)?.message
                    ?: "Failed to build fact-based cache for ${library.path}\n\n${t.message}\n\n${t.stackTraceToString()}"
            configuration.reportCompilationErrorAndThrow(message)
        } finally {
            factsDump.deleteIfExists()
        }
    }

    /** Logs `FACTS TIMING <phase> <ms> ms: <klib path>`, to measure the overhead of the facts-based analysis. */
    private fun reportTiming(phase: String, library: KotlinLibrary, startNanos: Long) {
        configuration.reportLog("FACTS TIMING $phase ${(System.nanoTime() - startNanos) / 1_000_000} ms: ${library.path}")
    }

    private inline fun <T> timed(phase: String, library: KotlinLibrary, block: () -> T): T {
        val start = System.nanoTime()
        return block().also { reportTiming(phase, library, start) }
    }

    companion object {
        const val CACHE_MANIFEST_FILE_NAME = "cache_manifest"
        const val CACHE_MANIFEST_HUMAN_READABLE_FILE_NAME = "$CACHE_MANIFEST_FILE_NAME.csv"
    }
}
