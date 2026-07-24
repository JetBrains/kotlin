/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.backend.konan.util.compilerFingerprint
import org.jetbrains.kotlin.backend.konan.util.reportCompilationErrorAndThrow
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.library.isExplicitlySpecifiedByUserInCLIArgument
import org.jetbrains.kotlin.konan.library.isImplicitlyLoadedFromKotlinNativeDistribution
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.library.unresolvedDependencies
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import org.jetbrains.kotlin.io.canonicalPathString
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

internal fun KotlinLibrary.getAllTransitiveDependencies(allLibraries: Map<String, KotlinLibrary>): List<KotlinLibrary> {
    val allDependencies = mutableSetOf<KotlinLibrary>()

    fun traverseDependencies(library: KotlinLibrary) {
        library.unresolvedDependencies.forEach {
            val dependency = allLibraries[it.path] ?: return@forEach
            if (dependency !in allDependencies) {
                allDependencies += dependency
                traverseDependencies(dependency)
            }
        }
    }

    traverseDependencies(this)
    return allDependencies.toList()
}

// TODO: deleteRecursively might throw an exception!
class CacheBuilder(
        val config: NativeSecondStageCompilationConfig,
        val compilationSpawner: CompilationSpawner
) {
    private val configuration = config.configuration
    private val autoCacheableFrom = configuration[NativeConfigurationKeys.AUTO_CACHEABLE_FROM]!!.map { Path(Path(it).canonicalPathString()) }
    private val icEnabled = configuration[CommonConfigurationKeys.INCREMENTAL_COMPILATION]!!
    private val includedLibraries = configuration.konanIncludedLibraries.toSet()
    private val generateTestRunner = configuration.getNotNull(NativeConfigurationKeys.GENERATE_TEST_RUNNER)

    fun needToBuild() = config.ignoreCacheReason == null
            && (config.isFinalBinary || config.produce.isFullCache)
            && (autoCacheableFrom.isNotEmpty() || icEnabled)

    private val allLibraries by lazy { config.resolvedLibraries.getFullList() }
    private val uniqueNameToLibrary by lazy { allLibraries.associateBy { it.uniqueName } }
    private val uniqueNameToHash = mutableMapOf<String, FingerprintHash>()

    private val caches = mutableMapOf<KotlinLibrary, CachedLibraries.Cache>()
    private val cacheRootDirectories = mutableMapOf<KotlinLibrary, String>()

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
        val monolithicallyCachedDependencies = library.getAllTransitiveDependencies(uniqueNameToLibrary).filter {
            !it.isCachedPerFile && !it.isImplicitlyLoadedFromKotlinNativeDistribution && !it.isNativeStdlib
        }
        return CachedLibraries.computeDependenciesFingerprint(monolithicallyCachedDependencies, uniqueNameToHash)
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

        allLibraries.forEach { library ->
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
            library.unresolvedDependencies.forEach dependenciesLoop@{
                val dependency = uniqueNameToLibrary[it.path] ?: return@dependenciesLoop
                dependableLibraries.getOrPut(dependency) { mutableListOf() }.add(library)
            }
        }

        externalLibrariesToCache.forEach { library ->
            val builtCachePaths = buildLibraryCache(library, true, emptyList())
            lastRebuiltArchives.addAll(builtCachePaths)
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
        // Caches produced by compilers older than 2.2.20 don't have the metadata at all; rebuild them as well (KT-87202).
        val staleCaches = icedLibraries.mapNotNull { library ->
            val cache = caches[library] ?: return@mapNotNull null
            staleCacheReason(library, cache)?.let { reason ->
                configuration.reportLog("Incremental cache for ${library.path} $reason; rebuilding it")
                library to cache
            }
        }

        // Unlike per-file caches, which are rebuilt in place file by file, a stale monolithic cache is deleted and rebuilt
        // right away: the dependent cache builds spawned below read it at its fixed name-keyed location.
        val [staleMonolithicCaches, stalePerFileCaches] = staleCaches.partition { it.second is CachedLibraries.Cache.Monolithic }
        staleMonolithicCaches.forEach { [library, cache] ->
            Path(cache.rootDirectory).deleteRecursively()
            lastRebuiltArchives.addAll(buildLibraryCache(library, false, emptyList()))
        }

        // Every library dependable on one of the changed external libraries needs its cache to be fully rebuilt.
        val needFullRebuild = findAllDependable(externalLibrariesToCache) + stalePerFileCaches.map { it.first }

        val libraryFilesWithFqNames = mutableMapOf<KotlinLibrary, List<FileWithFqName>>()

        val changedFiles = mutableListOf<LibraryFile>()
        val removedFiles = mutableListOf<LibraryFile>()
        val addedFiles = mutableListOf<LibraryFile>()
        val reversedPerFileDependencies = mutableMapOf<LibraryFile, MutableList<LibraryFile>>()
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
                                    if (!weak)
                                        reversedPerFileDependencies.getOrPut(LibraryFile(dependentLibrary, name)) { mutableListOf() }.add(libraryFile)
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

        for (library in icedLibraries) {
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
        val dependencies = library.getAllTransitiveDependencies(uniqueNameToLibrary)
        val dependencyCaches = dependencies.map {
            cacheRootDirectories[it] ?: run {
                configuration.reportLog("SKIPPING ${library.path} as some of the dependencies aren't cached")
                return emptyList()
            }
        }

        configuration.reportLog("CACHING ${library.path}")
        filesToCache.forEach { configuration.reportLog("    $it") }

        // Produce monolithic caches for external libraries for now, with the exception of the stdlib:
        // its cache is per-file by default (see [NativeSecondStageCompilationConfig.perFileCacheForStdlib]),
        // so when it has to be rebuilt here it must match the per-file layout the distribution ships.
        val makePerFileCache = !library.isCInteropLibrary() &&
                (!isExternal || (library.isNativeStdlib && config.perFileCacheForStdlib))

        val libraryCacheDirectory = when {
            library.isImplicitlyLoadedFromKotlinNativeDistribution || library.isNativeStdlib -> config.systemCacheDirectory
            isExternal -> CachedLibraries.computeLibraryCacheDirectory(
                    config.autoCacheDirectory, library, uniqueNameToLibrary, uniqueNameToHash)
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
     * Otherwise, acquires an OS-level file lock on [lockFile], calls [skipBuildAction] under the lock
     * to check whether the build should be skipped (e.g. the cache was already built by another process),
     * and if not, runs [buildAction] under the lock.
     * The lock file is created if it didn't exist before.
     *
     * Note: it is not absolutely necessary to always achieve mutual exclusion: the [buildAction] is
     * thread-safe and idempotent. The goal of the synchronization is to avoid memory pressure
     * caused by simultaneous build of the cache for a large library like stdlib.
     *
     * The lock file is intentionally **never deleted**. If you delete the lock file after
     * releasing the lock, this race appears:
     *  1. Process A holds a lock on `cache.lock`.
     *  2. Process B has already opened that file and is blocked waiting on the lock.
     *  3. Process A releases the lock and deletes `cache.lock`.
     *  4. Process C creates a new `cache.lock` and locks that new file.
     *  5. Process B acquires the lock on the old, now-unlinked inode it opened earlier.
     *
     * Now B and C both think they own "the" cache lock, but they are locking different
     * filesystem inodes. That breaks mutual exclusion.
     */
    private fun buildUnderFileLock(
            lockFile: Path?,
            skipBuildAction: () -> Boolean,
            buildAction: () -> Unit,
    ) {
        if (lockFile == null) {
            return buildAction()
        }

        FileChannel.open(
                lockFile.absolute(),
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        ).use { channel ->
            channel.acquireLockWithRetry().use {
                if (skipBuildAction()) return
                buildAction()
            }
        }
    }

    private fun FileChannel.acquireLockWithRetry(): FileLock {
        while (true) {
            try {
                return this.lock()
            } catch (_: OverlappingFileLockException) {
                // Another thread in the same JVM holds the lock. Just wait:
                // — if that thread dies with a crash, the whole process dies.
                // - if that thread fails with an exception, the lock is released.
                // - if that thread hangs, we might be tight on resources, so waiting is wise.
                Thread.sleep(200L)
            }
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
        try {
            // TODO: Run monolithic cache builds in parallel.
            spawnLibraryCacheBuild(library, dependencies, dependencyCaches, libraryCacheDirectory, makePerFileCache, filesToCache)
            cacheRootDirectories[library] = libraryCache.absolutePathString()
        } catch (t: Throwable) {
            try {
                libraryCache.deleteRecursively()
            } catch (_: Throwable) {
                // Nothing to do.
            }
            val message = (t as? CompilationErrorException)?.message
                    ?: run {
                        val workaround = when {
                            // The stdlib per-file cache is a system cache, not part of incremental compilation.
                            makePerFileCache && !library.isNativeStdlib ->
                                "incremental compilation (kotlin.incremental.native=false)"
                            makePerFileCache && library.isNativeStdlib ->
                                "stdlib per-file cache (-Xbinary=perFileCacheForStdlib=false)"
                            else ->
                                "compiler caches (https://kotl.in/disable-native-cache)"
                        }
                        @Suppress("IncorrectFormatting") val extraUserInfo =
                                """
                                    Failed to build cache for ${library.path}.
                                    As a workaround, please try to disable $workaround

                                    Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
                                    """.trimIndent()
                        "$extraUserInfo\n\n${t.message}\n\n${t.stackTraceToString()}"
                    }
            configuration.reportCompilationErrorAndThrow(message)
        }
    }

    private fun spawnLibraryCacheBuild(
            library: KotlinLibrary,
            dependencies: List<KotlinLibrary>,
            dependencyCaches: List<String>,
            libraryCacheDirectory: Path,
            makePerFileCache: Boolean,
            filesToCache: List<String>,
    ) {
        compilationSpawner.spawn(config.additionalCacheFlags /* TODO: Some way to put them directly to CompilerConfiguration? */) {
            config.configuration.konanHome?.let {
                this.konanHome = it
            }
            val libraryPath = library.path.absolutePathString()
            val libraries = dependencies.filter { it.isExplicitlySpecifiedByUserInCLIArgument }.map { it.path.absolutePathString() }
            val cachedLibraries = dependencies.zip(dependencyCaches).associate { it.first.path.absolutePathString() to it.second }
            configuration.reportLog(
                    "-p static_cache -Xadd-cache=${library.path} \\\n" +
                            libraries.joinToString("\n") { "-library $it \\" } + "\n" +
                            cachedLibraries.entries.joinToString("\n") { "-Xcached-library=${it.key},${it.value} \\" } + "\n" +
                            "-Xcache-directory=${libraryCacheDirectory.absolutePathString()}\n"
            )

            setupCommonOptionsForCaches(config)
            konanProducedArtifactKind = CompilerOutputKind.STATIC_CACHE
            // CHECK_DEPENDENCIES is computed based on outputKind, which is overwritten in the line above
            // So we have to change CHECK_DEPENDENCIES accordingly, otherwise they might not be downloaded (see KT-67547)
            checkDependencies = true
            konanLibraryToAddToCache = libraryPath
            konanNoDefaultLibs = true
            konanNoStdlib = true
            konanLibraries = libraries
            val generateTestRunner = this@CacheBuilder.generateTestRunner
            if (generateTestRunner != TestRunnerKind.NONE && libraryPath in this@CacheBuilder.includedLibraries) {
                konanFriendLibraries = config.friendModuleFiles.map { it.absolutePathString() }
                this.generateTestRunner = generateTestRunner
                konanIncludedLibraries = listOf(libraryPath)
                configuration.testDumpOutputPath?.let { testDumpOutputPath = it }
            }
            this.cachedLibraries = cachedLibraries
            cacheDirectories = listOf(libraryCacheDirectory.absolutePathString())
            this.makePerFileCache = makePerFileCache
            if (library.isSubjectOfIC)
                cachedLibraryDependenciesFingerprint = computeDependenciesFingerprint(library).toString()
            if (filesToCache.isNotEmpty())
                this.filesToCache = filesToCache
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
