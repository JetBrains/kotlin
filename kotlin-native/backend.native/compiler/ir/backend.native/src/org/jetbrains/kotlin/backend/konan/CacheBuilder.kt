/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.IdSignaturesExtractorFromKlibWithIndices
import org.jetbrains.kotlin.backend.common.IdSignaturesExtractorFromRegularKlib
import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.backend.konan.util.compilerFingerprint
import org.jetbrains.kotlin.backend.konan.util.reportCompilationErrorAndThrow
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.library.isExplicitlySpecifiedByUserInCLIArgument
import org.jetbrains.kotlin.konan.library.isImplicitlyLoadedFromKotlinNativeDistribution
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.newCompanionInitializationEnabled
import org.jetbrains.kotlin.library.uniqueName
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import org.jetbrains.kotlin.io.canonicalPathString
import org.jetbrains.kotlin.io.withExclusiveFileLock
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys.DEBUG_PREFIX_MAP
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.milliseconds

/** Libraries whose per-file caches are fully built by a single batched compilation, mapped from canonical paths to dependency fingerprints. */
internal val CACHE_BATCH = CompilerConfigurationKey.create<Map<String, FingerprintHash>>("per-file cache batch")

// TODO: deleteRecursively might throw an exception!
class CacheBuilder(
        val config: NativeSecondStageCompilationConfig,
        val compilationSpawner: CompilationSpawner
) {
    private val configuration = config.configuration
    private val autoCacheableFrom = configuration[NativeConfigurationKeys.AUTO_CACHEABLE_FROM]!!.map { Path(Path(it).canonicalPathString()) }
    private val icEnabled = configuration[CommonConfigurationKeys.INCREMENTAL_COMPILATION]!!
    private val includedLibraries = configuration.konanIncludedLibraries
            .map { Path(it).canonicalPathString() }.toSet()
    private val generateTestRunner = configuration.getNotNull(NativeConfigurationKeys.GENERATE_TEST_RUNNER)

    fun needToBuild() = config.ignoreCacheReason == null
            && (config.isFinalBinary || config.produce.isFullCache)
            && (autoCacheableFrom.isNotEmpty() || icEnabled)

    // Note: It's not totally clear, but likely the libraries in `uniqueNameToLibrary` should be in the reverse topo-order.
    private val uniqueNameToLibrary by lazy { config.klibDag.librariesReverseTopoSorted.associateBy { it.uniqueName } }

    private val caches = mutableMapOf<KotlinLibrary, CachedLibraries.Cache>()
    private val cacheRootDirectories = mutableMapOf<KotlinLibrary, String>()

    /**
     * What part of a library cache has been built during this compilation. See `wasRebuiltInThisRun`.
     */
    private sealed class RebuiltCache {
        /** The cache of the whole library. */
        object WholeLibrary : RebuiltCache()

        /** The per-file caches of the files with the given cache file IDs. */
        class Files : RebuiltCache() {
            val fileIds = mutableSetOf<String>()
        }
    }

    private val rebuiltCaches = mutableMapOf<KotlinLibrary, RebuiltCache>()

    /**
     * Whether the cache of the given file of [library] (or of the whole [library], if [fileId] is `null`) has been
     * built during this compilation.
     */
    fun wasRebuiltInThisRun(library: KotlinLibrary, fileId: String?): Boolean = when (val rebuiltCache = rebuiltCaches[library]) {
        null -> false
        RebuiltCache.WholeLibrary -> true
        is RebuiltCache.Files -> fileId in rebuiltCache.fileIds
    }

    // If libA depends on libB, then dependableLibraries[libB] contains libA.
    private val dependableLibraries = mutableMapOf<KotlinLibrary, MutableList<KotlinLibrary>>()

    private fun findAllDependable(libraries: List<KotlinLibrary>): Set<KotlinLibrary> {
        val visited = mutableSetOf<KotlinLibrary>()

        fun dfs(library: KotlinLibrary) {
            visited.add(library)
            dependableLibraries[library]?.forEach {
                if (it !in visited) dfs(it)
            }
        }

        libraries.forEach { if (it !in visited) dfs(it) }
        return visited
    }

    private data class LibraryFile(val library: KotlinLibrary, val file: String) {
        override fun toString() = "${library.uniqueName}|$file"
    }

    private val KotlinLibrary.isExternal: Boolean
        get() {
            val libraryCanonicalPath = Path(path.canonicalPathString())
            return autoCacheableFrom.any { libraryCanonicalPath.startsWith(it) }
        }

    private val KotlinLibrary.isSubjectOfIC: Boolean
        get() = isExplicitlySpecifiedByUserInCLIArgument && !isExternal && !isNativeStdlib

    // Among the IC'ed libraries only the cinterop ones are cached monolithically (see buildLibraryCache).
    private val KotlinLibrary.isCachedPerFile: Boolean
        get() = isSubjectOfIC && !isCInteropLibrary()

    // Only monolithically cached non-distribution dependencies (auto-cached external libraries and IC'ed cinterop libraries)
    // contribute to the fingerprint: changes in the per-file cached dependencies are tracked by the dirty-file analysis,
    // and the distribution libraries only change together with the compiler fingerprint (they live in compiler's dist directory).
    private fun computeDependenciesFingerprint(library: KotlinLibrary): FingerprintHash {
        val monolithicallyCachedDependencies = config.klibDag.getAllDependencies(library).filter {
            !it.isCachedPerFile && !it.isImplicitlyLoadedFromKotlinNativeDistribution && !it.isNativeStdlib
        }
        return CachedLibraries.computeDependenciesFingerprint(monolithicallyCachedDependencies)
    }

    private val currentCompilerFingerprint by lazy { config.distribution.compilerFingerprint }

    // Returns a human-readable reason if the cache of an IC'ed library cannot be reused, or null if it is up to date.
    private fun staleCacheReason(library: KotlinLibrary, cache: CachedLibraries.Cache): String? {
        val metadata = when (cache) {
            is CachedLibraries.Cache.Monolithic -> cache.getMetadataOrNull() // A cinterop library.
            is CachedLibraries.Cache.PerFile -> {
                // All files of a per-file cache are produced against the same compiler and dependencies,
                // so any file's metadata identifies the fingerprints of the whole cache.
                val anyCachedFile = Path(cache.path).listDirectoryEntries().firstOrNull()?.name ?: return null
                cache.getMetadataOrNull(anyCachedFile)
            }
        }
        return when {
            metadata == null ->
                "has no metadata (it was produced by a compiler older than 2.2.20)"
            metadata.compilerFingerprint != currentCompilerFingerprint ->
                "was produced by a different compiler version"
            metadata.dependenciesFingerprint != computeDependenciesFingerprint(library) ->
                "was produced against different dependencies"
            metadata.newCompanionInitialization != library.newCompanionInitializationEnabled ->
                "was produced with a different companion initialization order"
            cache is CachedLibraries.Cache.Monolithic
                    && metadata.hash != SerializedKlibFingerprint(library.path.toFile()).klibFingerprint ->
                "does not match the current content of the library"
            else -> null
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun build() {
        val externalLibrariesToCache = mutableListOf<KotlinLibrary>()
        val icedLibraries = mutableListOf<KotlinLibrary>()
        val lastRebuiltArchives = mutableListOf<Path>()

        // Note: The libraries should be in the reverse topo-order here!
        config.klibDag.librariesReverseTopoSorted.forEach { library ->
            // For MinGW target avoid compiling caches for anything except stdlib.
            if (config.target == KonanTarget.MINGW_X64 && !library.isNativeStdlib) {
                return@forEach
            }
            val isSubjectOfIC = library.isSubjectOfIC
            val cache = config.cachedLibraries.getLibraryCache(library, allowIncomplete = isSubjectOfIC)
            cache?.let {
                caches[library] = it
                cacheRootDirectories[library] = it.rootDirectory
            }
            if (isSubjectOfIC) {
                icedLibraries += library
            } else {
                if (cache == null) externalLibrariesToCache += library
            }
            config.klibDag.getDirectDependencies(library).forEach { dependency ->
                dependableLibraries.getOrPut(dependency) { mutableListOf() }.add(library)
            }
        }

        // [rebuiltLibraries] keeps every library built ahead of the per-library loop at the end, which then skips it.
        // The batched build also relies on it to tell which dependencies are already up to date at the moment of batching.
        val rebuiltLibraries = mutableSetOf<KotlinLibrary>()

        externalLibrariesToCache.forEach { library ->
            val builtCachePaths = buildLibraryCache(library, true, emptyList())
            if (builtCachePaths.isNotEmpty()) {
                rebuiltLibraries += library
                lastRebuiltArchives.addAll(builtCachePaths)
            }
        }

        if (!icEnabled) {
            dumpLastRebuiltArchivesToDisk(lastRebuiltArchives)
            return
        }

        // The incremental dirty-file check later on only compares the IR content hash of each source file. So switching to
        // a different compiler without running `clean` would silently reuse the bitcode produced by the previous compiler,
        // leading to linkage/runtime errors. Each cache records the producing compiler's fingerprint, so force a full rebuild
        // of any cached library whose fingerprint does not match the current compiler. (Distribution/auto caches live inside
        // the compiler distribution directory so they are naturally rebuilt with each compiler version.)
        // Similarly, the dirty-file check doesn't notice changes in external cached dependencies if their caches already exist.
        // This happens when a dependency is changed to an already-cached version, e.g. rolled back to the version it had before
        // an update (KT-87194). Each cache records the fingerprint of such dependencies, so force a full rebuild whenever
        // it doesn't match the current dependencies.
        // The dirty-file check doesn't cover the monolithically cached cinterop libraries either: their caches are looked up
        // by the library name, so a changed library would silently reuse the stale cache (KT-87273). Each cache records
        // the fingerprint of its library, so force a rebuild whenever it doesn't match the current library.
        // The dirty-file check doesn't notice the library-wide switch of the companion initialization order either: it is
        // only recorded in the klib manifest (with `-Xcompanion-blocks`), yet affects the code generated for every file
        // of the library (KT-89390). Each cache records the order it was produced with, so force a full rebuild on a mismatch.
        // Caches produced by compilers older than 2.2.20 don't have the metadata at all; rebuild them as well (KT-87202).
        val staleCaches = icedLibraries.mapNotNull { library ->
            val cache = caches[library] ?: return@mapNotNull null
            staleCacheReason(library, cache)?.let { reason ->
                configuration.reportLog("Incremental cache for ${library.path} $reason; rebuilding it")
                library to cache
            }
        }

        // Unlike per-file caches, which are rebuilt in place file by file, a stale monolithic cache is deleted
        // and then rebuilt from scratch.
        val [staleMonolithicCaches, stalePerFileCaches] = staleCaches.partition { it.second is CachedLibraries.Cache.Monolithic }
        staleMonolithicCaches.forEach { [library, cache] ->
            Path(cache.rootDirectory).deleteRecursively()
            caches.remove(library)
            cacheRootDirectories.remove(library)
        }

        // Build all the monolithic (cinterop) caches right away: the dependent cache builds spawned below,
        // including the batched one, read them at their fixed name-keyed locations.
        for (library in icedLibraries) {
            if (library.isCachedPerFile || caches[library] != null) continue
            val builtCachePaths = buildLibraryCache(library, false, emptyList())
            if (builtCachePaths.isNotEmpty()) {
                rebuiltLibraries += library
                lastRebuiltArchives.addAll(builtCachePaths)
            }
        }

        // Every library dependable on one of the changed external libraries needs its cache to be fully rebuilt.
        val needFullRebuild = findAllDependable(externalLibrariesToCache) + stalePerFileCaches.map { it.first }

        val libraryFilesWithFqNames = mutableMapOf<KotlinLibrary, List<FileWithFqName>>()

        val changedFiles = mutableListOf<LibraryFile>()
        val removedFiles = mutableListOf<LibraryFile>()
        val addedFiles = mutableListOf<LibraryFile>()
        val reversedPerFileDependencies = mutableMapOf<LibraryFile, MutableList<LibraryFile>>()
        val reversedWeakPerFileDependencies = mutableMapOf<LibraryFile, MutableList<LibraryFile>>()
        val reversedWholeLibraryDependencies = mutableMapOf<KotlinLibrary, MutableList<LibraryFile>>()
        for (library in icedLibraries) {
            if (library in needFullRebuild) continue
            val cache = caches[library] ?: continue
            if (cache !is CachedLibraries.Cache.PerFile) {
                require(library.isCInteropLibrary())
                continue
            }

            val libraryCacheRootDir = Path(cache.path)
            val cachedFiles = libraryCacheRootDir.listDirectoryEntries().map { it.name }

            val actualFilesWithFqNames = library.getFilesWithFqNames()
            libraryFilesWithFqNames[library] = actualFilesWithFqNames
            val actualFiles = actualFilesWithFqNames.withIndex()
                    .associate { CacheSupport.cacheFileId(it.value.fqName, it.value.filePath) to it.index }
                    .toMutableMap()

            for (cachedFile in cachedFiles) {
                val libraryFile = LibraryFile(library, cachedFile)
                val fileIndex = actualFiles[cachedFile]
                if (fileIndex == null) {
                    removedFiles.add(libraryFile)
                } else {
                    actualFiles.remove(cachedFile)
                    val actualContentHash = SerializedIrFileFingerprint(library, fileIndex).fileFingerprint
                    // Missing metadata (a cache produced by a compiler older than 2.2.20) means the file has to be rebuilt.
                    val previousContentHash = cache.getMetadataOrNull(cachedFile)?.hash
                    // A recorded dependency on a library that is not among the resolved libraries anymore
                    // (e.g. the module was removed from the project after the cache had been built, KT-61644)
                    // also means the file has to be rebuilt (the cache references a missing library which would lead
                    // to a linkage error otherwise); there is nothing to propagate through such an edge.
                    val [knownDependencies, removedDependencies] = cache.getFileDependencies(cachedFile)
                            .partition { it.libName in uniqueNameToLibrary }
                    if (previousContentHash != actualContentHash || removedDependencies.isNotEmpty())
                        changedFiles.add(libraryFile)

                    for (dependency in knownDependencies) {
                        val dependentLibrary = uniqueNameToLibrary[dependency.libName]!!
                        when (val kind = dependency.kind) {
                            is DependenciesTracker.DependencyKind.WholeModule ->
                                reversedWholeLibraryDependencies.getOrPut(dependentLibrary) { mutableListOf() }.add(libraryFile)
                            is DependenciesTracker.DependencyKind.CertainFiles ->
                                kind.files.forEach { (name, weak) ->
                                    (if (weak) reversedWeakPerFileDependencies else reversedPerFileDependencies)
                                            .getOrPut(LibraryFile(dependentLibrary, name)) { mutableListOf() }.add(libraryFile)
                                }
                        }
                    }
                }
            }
            for (newFile in actualFiles.keys)
                addedFiles.add(LibraryFile(library, newFile))
        }

        configuration.reportLog("IC analysis results")
        configuration.reportLog("    CACHED:")
        icedLibraries.filter { caches[it] != null }.forEach { configuration.reportLog("        ${it.path}") }
        configuration.reportLog("    CLEAN BUILD:")
        icedLibraries.filter { caches[it] == null }.forEach { configuration.reportLog("        ${it.path}") }
        configuration.reportLog("    FULL REBUILD:")
        icedLibraries.filter { it in needFullRebuild }.forEach { configuration.reportLog("        ${it.path}") }
        configuration.reportLog("    ADDED FILES:")
        addedFiles.forEach { configuration.reportLog("        $it") }
        configuration.reportLog("    REMOVED FILES:")
        removedFiles.forEach { configuration.reportLog("        $it") }
        configuration.reportLog("    CHANGED FILES:")
        changedFiles.forEach { configuration.reportLog("        $it") }

        val dirtyFiles = mutableSetOf<LibraryFile>()

        fun dfs(libraryFile: LibraryFile) {
            dirtyFiles += libraryFile
            reversedPerFileDependencies[libraryFile]?.forEach {
                if (it !in dirtyFiles) dfs(it)
            }
        }

        removedFiles.forEach {
            if (it !in dirtyFiles) dfs(it)
            // A weak dependency is link-time only, but the walk over cached dependencies still resolves it,
            // so a file that weakly depends on a removed file must be rebuilt to drop the stale edge.
            reversedWeakPerFileDependencies[it]?.forEach { weakDependent ->
                if (weakDependent !in dirtyFiles) dfs(weakDependent)
            }
        }
        changedFiles.forEach {
            if (it !in dirtyFiles) dfs(it)
        }
        dirtyFiles.addAll(addedFiles)

        removedFiles.forEach {
            dirtyFiles.remove(it)
            Path(caches[it.library]!!.rootDirectory).resolve(it.file).deleteRecursively()
        }

        val groupedDirtyFiles = dirtyFiles.groupBy { it.library }
        configuration.reportLog("    DIRTY FILES:")
        groupedDirtyFiles.values.flatten().forEach {
            configuration.reportLog("        $it")
        }

        if (config.batchedCacheBuild) {
            tryBuildCacheBatch(icedLibraries, needFullRebuild, groupedDirtyFiles.keys, rebuiltLibraries)
                    ?.let { [batchLibraries, builtCachePaths] ->
                        rebuiltLibraries.addAll(batchLibraries)
                        lastRebuiltArchives.addAll(builtCachePaths)
                    }
        }

        for (library in icedLibraries) {
            if (library in rebuiltLibraries) continue
            val filesToCache = groupedDirtyFiles[library]?.let { libraryFiles ->
                val filesWithFqNames = libraryFilesWithFqNames[library]!!.associateBy {
                    CacheSupport.cacheFileId(it.fqName, it.filePath)
                }
                libraryFiles.map { filesWithFqNames[it.file]!!.filePath }
            }.orEmpty()

            val isFirstBuild = caches[library] == null

            val builtCachePaths = when {
                library in needFullRebuild -> {
                    buildLibraryCache(library, false, emptyList())
                }
                isFirstBuild || filesToCache.isNotEmpty() -> {
                    buildLibraryCache(library, false, filesToCache)
                }
                else -> emptyList()
            }

            lastRebuiltArchives.addAll(builtCachePaths)
        }

        dumpLastRebuiltArchivesToDisk(lastRebuiltArchives)
    }

    /**
     * Builds in spawned batched compilations the per-file caches of all the libraries that need a full rebuild:
     * the ones having no cache at all (on a clean build that is every library) and the ones from [needFullRebuild].
     *
     * A batched compilation creates the IR linker once and thus deserializes the common dependencies
     * (like stdlib) once instead of multiple times (once per library).
     *
     * The linker maps every signature to a single declaration, and all the libraries of a batch are deserialized exhaustively,
     * so two libraries sharing a top-level signature (e.g. identical `internal` top-level functions in the same package) would clash
     * with "symbol is already bound". Such libraries are put into separate batches, built one after another in the dependency order.
     *
     * Returns the libraries built together with their binary archives, or null if batching is not applicable.
     */
    private fun tryBuildCacheBatch(
            libraries: List<KotlinLibrary>,
            needFullRebuild: Set<KotlinLibrary>,
            dirtyLibraries: Set<KotlinLibrary>,
            rebuiltLibraries: Set<KotlinLibrary>,
    ): Pair<List<KotlinLibrary>, List<Path>>? {
        // The test runner is generated in a dedicated compilation of each of the [includedLibraries],
        // so these are left to the per-library builds.
        val candidates = libraries.filter {
            it.isCachedPerFile && (it !in caches || it in needFullRebuild)
                    && (generateTestRunner == TestRunnerKind.NONE || it.canonicalPath.pathString !in includedLibraries)
        }
        if (candidates.size < 2) return null // No benefit from batching build.
        val candidatesSet = candidates.toSet()
        // A batch member may only depend on other members and on up-to-date caches. A dependency with pending
        // rebuilds must be rebuilt before its dependents (which the per-library loop does by following
        // the reverse topo-order), so the dependents of such a library cannot be batched.
        val batch = candidates.filter { candidate ->
            config.klibDag.getAllDependencies(candidate).all { dependency ->
                dependency in candidatesSet || dependency in rebuiltLibraries
                        || (dependency in cacheRootDirectories && dependency !in dirtyLibraries && dependency !in needFullRebuild)
            }
        }
        val batchSet = batch.toSet()
        (candidates - batchSet).takeIf { it.isNotEmpty() }?.let { excluded ->
            configuration.reportLog("EXCLUDED FROM BATCH (a dependency has pending rebuilds): ${excluded.joinToString { it.uniqueName }}")
        }
        if (batch.size < 2) return null

        val groups = splitIntoNonConflictingGroups(batch, batchSet)
        if (groups.all { it.size < 2 }) return null // Nothing left to batch.

        val builtLibraries = mutableListOf<KotlinLibrary>()
        val builtArchives = mutableListOf<Path>()
        for (group in groups) {
            val archives = buildCacheBatch(group)
            builtLibraries.addAll(group)
            builtArchives.addAll(archives)
        }
        return builtLibraries to builtArchives
    }

    /**
     * Splits [batch] (in the reverse topo-order) into groups having no common cross-library top-level signatures.
     * The groups are ordered so that each library goes after all its dependencies from [batch]: a library joins
     * the first group it does not conflict with among the groups not preceding those of its dependencies.
     *
     * The check covers everything a group's compilation loads, i.e. the members together with their (cached)
     * dependencies: a dependency with the same signature as a library from another member's closure would be resolved
     * to whichever of the two got deserialized first (e.g. inside a cached inline function body), silently binding to the
     * wrong body. Each per-library build loads only its own closure, so a clash within a single closure is pre-existing
     * and not of the batching's concern. However, which of the clashing declarations gets reached depends on what else
     * is compiled alongside (only one of them might be reached by the library's own build). Thus, a group whose loaded
     * libraries share a signature conservatively doesn't grow any further.
     */
    private fun splitIntoNonConflictingGroups(batch: List<KotlinLibrary>, batchSet: Set<KotlinLibrary>): List<List<KotlinLibrary>> {
        class Group {
            val libraries = mutableListOf<KotlinLibrary>()
            val loadedLibraries = mutableSetOf<KotlinLibrary>()
            val signatureOwners = mutableMapOf<IdSignature, KotlinLibrary>()
            var sealed = false // Some of the loaded libraries share a signature.
        }

        val signaturesCache = mutableMapOf<KotlinLibrary, Set<IdSignature>>()
        fun KotlinLibrary.signatures() = signaturesCache.getOrPut(this) { getTopLevelNonPrivateSignatures() }

        fun KotlinLibrary.loadedClosure(): List<KotlinLibrary> =
                listOf(this) + config.klibDag.getAllDependencies(this).filter {
                    !it.isImplicitlyLoadedFromKotlinNativeDistribution && !it.isNativeStdlib && !it.isCInteropLibrary()
                }

        // The groups are built in order (groups[0] is built first, groups[1] is the next, ...)
        val groups = mutableListOf(Group()) // The list always contains one dummy group at the end to not bother with empty list.
        val groupIndices = mutableMapOf<KotlinLibrary, Int>()
        for (library in batch) {
            val closure = library.loadedClosure()
            val firstAllowedIndex = config.klibDag.getAllDependencies(library)
                    .filter { it in batchSet }
                    // A library can't be placed into a group built before its dependencies.
                    .maxOfOrNull { groupIndices.getValue(it) } ?: 0
            var groupIdx = firstAllowedIndex
            do {
                val group = groups[groupIdx++]
                var hasConflicts = group.sealed
                if (hasConflicts) continue // A sealed group has conflicts by definition.
                var index = 0
                do {
                    val loaded = closure[index++]
                    if (loaded in group.loadedLibraries) continue // The already loaded libraries are the same ones, not clashing.
                    val conflict = loaded.signatures().firstOrNull { it in group.signatureOwners } ?: continue
                    hasConflicts = true
                    val owner = group.signatureOwners[conflict]!!
                    val ownerMember = group.libraries.first { owner == it || owner in config.klibDag.getAllDependencies(it) }
                    configuration.reportLog(
                            "SPLITTING BATCH: ${library.uniqueName} and ${ownerMember.uniqueName} share a top-level signature: $conflict" +
                                    (if (loaded != library || owner != ownerMember) " (via ${loaded.uniqueName} and ${owner.uniqueName})" else ""))
                } while (index < closure.size && !hasConflicts)
            } while (groupIdx < groups.size && hasConflicts)
            if (groupIdx == groups.size)
                groups.add(Group()) // The dummy group has been used - add a new one.
            val group = groups[--groupIdx]
            group.libraries.add(library)
            groupIndices[library] = groupIdx
            // Note: A clash inside the closure itself is pre-existing (see above), so the closure is added unconditionally,
            // but the group gets sealed.
            for (loaded in closure) {
                if (!group.loadedLibraries.add(loaded)) continue // Already added by other group's member.
                for (signature in loaded.signatures()) {
                    // Note: although here group-wide signatureOwners is used, a conflict can only be inside the closure because
                    // otherwise it would have been found in the previous loop and current group wouldn't have been picked.
                    val owner = group.signatureOwners.putIfAbsent(signature, loaded) ?: continue
                    if (!group.sealed) {
                        group.sealed = true
                        configuration.reportLog(
                                "SEALING BATCH: ${library.uniqueName} loads ${owner.uniqueName} and ${loaded.uniqueName}" +
                                        " sharing a top-level signature: $signature")
                    }
                }
            }
        }

        groups.removeLast() // Remove the (empty) dummy group.
        return groups.map { it.libraries }
    }

    /**
     * The signatures of the library's top-level declarations that might clash across libraries.
     */
    private fun KotlinLibrary.getTopLevelNonPrivateSignatures(): Set<IdSignature> =
            IdSignaturesExtractorFromKlibWithIndices(this, IdSignaturesExtractorFromRegularKlib(this))
                    .extractOnlyTopLevelPublicSignatures() // Internal are also included in the result.
                    .declaredSignatures

    /** Builds the per-file caches of [batch] in a single spawned compilation and returns their binary archives. */
    @OptIn(ExperimentalPathApi::class)
    private fun buildCacheBatch(batch: List<KotlinLibrary>): List<Path> {
        val batchSet = batch.toSet()
        val cacheDirectory = config.incrementalCacheDirectory!!
        val outputs = batch.associateWith { cacheDirectory.resolve(CachedLibraries.getPerFileCachedLibraryName(it)) }
        val dependencies = batch.flatMap { config.klibDag.getAllDependencies(it) }.distinct().filter { it !in batchSet }
        // The members are built from scratch: drop the stale caches of the [needFullRebuild] ones
        // as well as any incomplete leftovers of an interrupted build.
        outputs.forEach { [library, output] ->
            output.deleteRecursively()
            caches.remove(library)
        }
        cacheDirectory.createDirectories()
        configuration.reportLog("CACHING BATCH: ${batch.joinToString { it.uniqueName }}")
        configuration.reportLog(batch.joinToString("\n") { it.canonicalPath.pathString })
        trySpawningCacheBuild(
                libraries = batch,
                dependencies = dependencies,
                dependencyCaches = dependencies.map { cacheRootDirectories.getValue(it) },
                cacheDirectory = cacheDirectory,
                makePerFileCache = true,
                outputs = outputs.values.toList(),
        ) {
            put(CACHE_BATCH, batch.associate { it.canonicalPath.pathString to computeDependenciesFingerprint(it) })
            putIfNotNull(DEBUG_PREFIX_MAP, config.configuration[DEBUG_PREFIX_MAP])
        }

        val rebuiltArchives = mutableListOf<Path>()
        outputs.forEach { [library, output] ->
            cacheRootDirectories[library] = output.absolutePathString()
            rebuiltArchives.addAll(library.getPerFileCachedBinaryFilePaths(output, emptyList()))

            recordRebuiltCacheFiles(library, makePerFileCache = true, emptyList())
        }
        return rebuiltArchives
    }

    private fun KotlinLibrary.getPerFileCachedBinaryFilePaths(cacheRoot: Path, filesToCache: List<String>): List<Path> {
        // Restrict to the files rebuilt this run (empty = whole-library build) so the IC build output doesn't list untouched files as rebuilt.
        return getFilesWithFqNames()
                .filter { filesToCache.isEmpty() || it.filePath in filesToCache }
                .map { fqnFile ->
                    val fileId = CacheSupport.cacheFileId(fqnFile.fqName, fqnFile.filePath)
                    cacheRoot.resolve(fileId).resolve("bin").resolve("lib$fileId.a")
                }
    }

    private fun KotlinLibrary.getMonolithicCachedBinaryFilePaths(cacheRoot: Path): List<Path> {
        val libraryFilename = "lib${CachedLibraries.getCachedLibraryName(this)}.a"
        return listOf(cacheRoot.resolve("bin").resolve(libraryFilename))
    }

    private fun buildLibraryCache(library: KotlinLibrary, isExternal: Boolean, filesToCache: List<String>): List<Path> {
        val dependencies = config.klibDag.getAllDependencies(library).toList()
        val dependencyCaches = dependencies.map {
            cacheRootDirectories[it] ?: run {
                configuration.reportLog("SKIPPING ${library.path} as some of the dependencies aren't cached")
                return emptyList()
            }
        }

        configuration.reportLog("CACHING ${library.path}")
        filesToCache.forEach { configuration.reportLog("    $it") }

        // Produce monolithic caches for external libraries for now, except debug stdlib:
        // its cache is per-file by default (see [NativeSecondStageCompilationConfig.perFileCacheForStdlib]),
        // so when it has to be rebuilt here it must match the per-file layout the distribution ships.
        val makePerFileCache = !library.isCInteropLibrary() &&
                (!isExternal || (library.isNativeStdlib && config.perFileCacheForStdlib)) &&
                !(library.isNativeStdlib && config.optimizationsEnabled)

        val libraryCacheDirectory = when {
            library.isImplicitlyLoadedFromKotlinNativeDistribution || library.isNativeStdlib -> config.systemCacheDirectory
            isExternal -> CachedLibraries.computeLibraryCacheDirectory(
                    config.autoCacheDirectory, library, config.klibDag)
            else -> config.incrementalCacheDirectory!!
        }
        val libraryCache = libraryCacheDirectory.resolve(
                if (makePerFileCache)
                    CachedLibraries.getPerFileCachedLibraryName(library)
                else
                    CachedLibraries.getCachedLibraryName(library)
        )
        libraryCacheDirectory.createDirectories()

        /*
         * Use lock file to not allow caches building in parallel. Actually, this is OK (there are some synchronization
         * mechanisms in the compiler) but may take up a lot of memory (especially when building stdlib cache). In particular,
         * this happens during some tests which specify certain binary options which won't allow to use the precompiled caches.
         */
        val lockFileName = "${libraryCache.absolutePathString()}.lock"
        val lockFile = if (isExternal) {
            // External (system/auto) caches are shared between processes and may be built
            // in parallel so guard their construction with a file lock.
            Path(lockFileName)
        } else {
            // Incremental caches live in a per-project directory and are never built in parallel.
            null
        }

        buildUnderFileLock(lockFile, skipBuildAction = {
            libraryCache.exists().also {
                if (it) cacheRootDirectories[library] = libraryCache.absolutePathString()
            }
        }) {
            tryBuildingLibraryCache(library, dependencies, dependencyCaches, libraryCacheDirectory, makePerFileCache, filesToCache, libraryCache)
        }

        val cacheRootPath = libraryCache.absolute()

        return if (makePerFileCache) {
            library.getPerFileCachedBinaryFilePaths(cacheRootPath, filesToCache)
        } else {
            library.getMonolithicCachedBinaryFilePaths(cacheRootPath)
        }
    }

    /**
     * If [lockFile] is `null`, performs [buildAction] without any synchronization.
     * Otherwise, acquires an OS-level file lock on [lockFile] (see [withExclusiveFileLock]), calls [skipBuildAction]
     * under the lock to check whether the build should be skipped (e.g. the cache was already built by another process),
     * and if not, runs [buildAction] under the lock.
     *
     * Note: it is not absolutely necessary to always achieve mutual exclusion: the [buildAction] is
     * thread-safe and idempotent. The goal of the synchronization is to avoid memory pressure
     * caused by simultaneous build of the cache for a large library like stdlib.
     */
    private fun buildUnderFileLock(
            lockFile: Path?,
            skipBuildAction: () -> Boolean,
            buildAction: () -> Unit,
    ) {
        if (lockFile == null) {
            return buildAction()
        }

        withExclusiveFileLock(lockFile, retryTimeout = 200.milliseconds) {
            if (!skipBuildAction()) buildAction()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun tryBuildingLibraryCache(
            library: KotlinLibrary,
            dependencies: List<KotlinLibrary>,
            dependencyCaches: List<String>,
            libraryCacheDirectory: Path,
            makePerFileCache: Boolean,
            filesToCache: List<String>,
            libraryCache: Path,
    ) {
        configuration.reportLog(
                "-p static_cache -Xadd-cache=${library.path} \\\n" +
                        dependencies.joinToString("\n") { "-library ${it.canonicalPath.pathString} \\" } + "\n" +
                        dependencies.zip(dependencyCaches).joinToString("\n") { [dependency, dependencyCache] ->
                            "-Xcached-library=${dependency.canonicalPath.pathString},$dependencyCache \\"
                        } + "\n" +
                        "-Xcache-directory=${libraryCacheDirectory.absolutePathString()}\n"
        )
        // TODO: Run monolithic cache builds in parallel.
        trySpawningCacheBuild(
                listOf(library),
                dependencies,
                dependencyCaches,
                libraryCacheDirectory,
                makePerFileCache,
                listOf(libraryCache),
        ) {
            val libraryPath = library.canonicalPath.pathString
            konanLibraryToAddToCache = libraryPath
            val generateTestRunner = this@CacheBuilder.generateTestRunner
            if (generateTestRunner != TestRunnerKind.NONE && libraryPath in this@CacheBuilder.includedLibraries) {
                konanFriendLibraries = config.loadedKlibs.friends.map { it.canonicalPath.pathString }
                this.generateTestRunner = generateTestRunner
                konanIncludedLibraries = listOf(libraryPath)
                configuration.testDumpOutputPath?.let { testDumpOutputPath = it }
            }
            if (library.isSubjectOfIC) {
                cachedLibraryDependenciesFingerprint = computeDependenciesFingerprint(library).toString()
                putIfNotNull(DEBUG_PREFIX_MAP, config.configuration[DEBUG_PREFIX_MAP])
            }
            if (filesToCache.isNotEmpty())
                this.filesToCache = filesToCache
        }

        cacheRootDirectories[library] = libraryCache.absolutePathString()

        recordRebuiltCacheFiles(library, makePerFileCache, filesToCache)
    }

    /**
     * Spawns a compilation building the caches of [libraries] (located at [outputs]) in [cacheDirectory]
     * against the [dependencies] cached at [dependencyCaches].
     * [setupConfiguration] adds the options specific to the particular build.
     */
    @OptIn(ExperimentalPathApi::class)
    private fun trySpawningCacheBuild(
            libraries: List<KotlinLibrary>,
            dependencies: List<KotlinLibrary>,
            dependencyCaches: List<String>,
            cacheDirectory: Path,
            makePerFileCache: Boolean,
            outputs: List<Path>,
            setupConfiguration: CompilerConfiguration.() -> Unit,
    ) {
        try {
            compilationSpawner.spawn(config.additionalCacheFlags /* TODO: Some way to put them directly to CompilerConfiguration? */) {
                config.configuration.konanHome?.let { konanHome = it }
                setupCommonOptionsForCaches(config)
                konanProducedArtifactKind = CompilerOutputKind.STATIC_CACHE
                // CHECK_DEPENDENCIES is computed based on outputKind, which is overwritten in the line above
                // So we have to change CHECK_DEPENDENCIES accordingly, otherwise they might not be downloaded (see KT-67547)
                checkDependencies = true
                konanNoDefaultLibs = true
                konanNoStdlib = true
                konanLibraries = (dependencies + libraries).map { it.canonicalPath.pathString }
                cachedLibraries = dependencies.zip(dependencyCaches).associate { [dependency, dependencyCache] ->
                    dependency.canonicalPath.pathString to dependencyCache
                }
                cacheDirectories = listOf(cacheDirectory.absolutePathString())
                this.makePerFileCache = makePerFileCache
                serializedKlibDag = config.klibDag.serialize() // Put the DAG of dependencies to compiler configuration to avoid re-computing it again.
                setupConfiguration()
            }
        } catch (t: Throwable) {
            outputs.forEach {
                try {
                    it.deleteRecursively()
                } catch (_: Throwable) {
                    // Nothing to do.
                }
            }
            val message = (t as? CompilationErrorException)?.message
                    ?: run {
                        val workaround = when {
                            // The stdlib per-file cache is a system cache, not part of incremental compilation.
                            makePerFileCache && libraries.singleOrNull()?.isNativeStdlib == true ->
                                "stdlib per-file cache (-Xbinary=perFileCacheForStdlib=false)"
                            makePerFileCache ->
                                "incremental compilation (kotlin.incremental.native=false)"
                            else ->
                                "compiler caches (https://kotl.in/disable-native-cache)"
                        }
                        val cacheDescription = libraries.singleOrNull()?.let { "cache for ${it.path}" }
                                ?: "batched cache for [${libraries.joinToString { it.uniqueName }}]"
                        @Suppress("IncorrectFormatting") val extraUserInfo =
                                """
                                    Failed to build $cacheDescription.
                                    As a workaround, please try to disable $workaround

                                    Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
                                    """.trimIndent()
                        "$extraUserInfo\n\n${t.message}\n\n${t.stackTraceToString()}"
                    }
            configuration.reportCompilationErrorAndThrow(message)
        }
    }

    private fun recordRebuiltCacheFiles(library: KotlinLibrary, makePerFileCache: Boolean, filesToCache: List<String>): Unit = when {
        makePerFileCache && filesToCache.isNotEmpty() -> {
            val rebuiltFiles = when (val rebuiltCache = rebuiltCaches.getOrPut(library) { RebuiltCache.Files() }) {
                RebuiltCache.WholeLibrary -> return // Already covers every file of the library.
                is RebuiltCache.Files -> rebuiltCache.fileIds
            }
            library.getFilesWithFqNames()
                    .filter { it.filePath in filesToCache }
                    .forEach { rebuiltFiles += CacheSupport.cacheFileId(it.fqName, it.filePath) }
        }
        else -> {
            rebuiltCaches[library] = RebuiltCache.WholeLibrary
        }
    }

    private fun dumpLastRebuiltArchivesToDisk(lastRebuiltArchives: List<Path>) {
        config.dumpBuiltCachesTo?.let { outputPath ->
            val rebuiltArchivesFile = Path(outputPath)
            val fileContent = if (lastRebuiltArchives.isEmpty())
                ""
            else
                lastRebuiltArchives.joinToString("\n") { it.absolutePathString() } + "\n"
            rebuiltArchivesFile.writeText(fileContent)
        }
    }
}
