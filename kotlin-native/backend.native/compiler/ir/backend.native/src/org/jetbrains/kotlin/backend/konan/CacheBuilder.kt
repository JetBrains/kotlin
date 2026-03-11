/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.config.cacheDirectories
import org.jetbrains.kotlin.konan.config.cachedLibraries
import org.jetbrains.kotlin.konan.config.checkDependencies
import org.jetbrains.kotlin.konan.config.filesToCache
import org.jetbrains.kotlin.konan.config.generateTestRunner
import org.jetbrains.kotlin.konan.config.konanFriendLibraries
import org.jetbrains.kotlin.konan.config.konanIncludedLibraries
import org.jetbrains.kotlin.konan.config.konanLibraries
import org.jetbrains.kotlin.konan.config.konanLibraryToAddToCache
import org.jetbrains.kotlin.konan.config.konanNoDefaultLibs
import org.jetbrains.kotlin.konan.config.konanNoEndorsedLibs
import org.jetbrains.kotlin.konan.config.konanNoStdlib
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.config.makePerFileCache
import org.jetbrains.kotlin.konan.config.testDumpOutputPath
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import org.jetbrains.kotlin.backend.common.legacyKlibReverseTopoSort

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
    private companion object {
        const val lockRetrySleepMillis = 25L
    }

    private val configuration = config.configuration
    private val autoCacheableFrom = configuration[NativeConfigurationKeys.AUTO_CACHEABLE_FROM]!!.map { File(it) }
    private val icEnabled = configuration[CommonConfigurationKeys.INCREMENTAL_COMPILATION]!!
    private val includedLibraries = configuration.konanIncludedLibraries.toSet()
    private val generateTestRunner = configuration.getNotNull(NativeConfigurationKeys.GENERATE_TEST_RUNNER)

    fun needToBuild() = config.ignoreCacheReason == null
            && (config.isFinalBinary || config.produce.isFullCache)
            && (autoCacheableFrom.isNotEmpty() || icEnabled)

    private val allLibraries by lazy { config.resolvedLibraries.getFullList().legacyKlibReverseTopoSort() }
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

    private val KotlinLibrary.isExternal
        get() = autoCacheableFrom.any { libraryFile.canonicalFile.startsWith(it.canonicalFile) }

    fun build() {
        val externalLibrariesToCache = mutableListOf<KotlinLibrary>()
        val icedLibraries = mutableListOf<KotlinLibrary>()

        allLibraries.forEach { library ->
            // For MinGW target avoid compiling caches for anything except stdlib.
            if (config.target == KonanTarget.MINGW_X64 && !library.isNativeStdlib) {
                return@forEach
            }
            val isSubjectOfIC = !library.isFromKotlinNativeDistribution && !library.isExternal && !library.isNativeStdlib
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

        externalLibrariesToCache.forEach { buildLibraryCache(it, true, emptyList()) }

        if (!icEnabled) return

        // Every library dependable on one of the changed external libraries needs its cache to be fully rebuilt.
        val needFullRebuild = findAllDependable(externalLibrariesToCache)

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

            val libraryCacheRootDir = File(cache.path)
            val cachedFiles = libraryCacheRootDir.listFiles.map { it.name }

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
                    val previousContentHash = cache.getMetadata(cachedFile).hash
                    if (previousContentHash != actualContentHash)
                        changedFiles.add(libraryFile)

                    val dependencies = cache.getFileDependencies(cachedFile)
                    for (dependency in dependencies) {
                        val dependentLibrary = uniqueNameToLibrary[dependency.libName]
                                ?: error("Unknown dependent library ${dependency.libName}")
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

        configuration.report(CompilerMessageSeverity.LOGGING, "IC analysis results")
        configuration.report(CompilerMessageSeverity.LOGGING, "    CACHED:")
        icedLibraries.filter { caches[it] != null }.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        ${it.location}") }
        configuration.report(CompilerMessageSeverity.LOGGING, "    CLEAN BUILD:")
        icedLibraries.filter { caches[it] == null }.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        ${it.location}") }
        configuration.report(CompilerMessageSeverity.LOGGING, "    FULL REBUILD:")
        icedLibraries.filter { it in needFullRebuild }.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        ${it.location}") }
        configuration.report(CompilerMessageSeverity.LOGGING, "    ADDED FILES:")
        addedFiles.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        $it") }
        configuration.report(CompilerMessageSeverity.LOGGING, "    REMOVED FILES:")
        removedFiles.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        $it") }
        configuration.report(CompilerMessageSeverity.LOGGING, "    CHANGED FILES:")
        changedFiles.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        $it") }

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
            File(caches[it.library]!!.rootDirectory).child(it.file).deleteRecursively()
        }

        val groupedDirtyFiles = dirtyFiles.groupBy { it.library }
        configuration.report(CompilerMessageSeverity.LOGGING, "    DIRTY FILES:")
        groupedDirtyFiles.values.flatten().forEach {
            configuration.report(CompilerMessageSeverity.LOGGING, "        $it")
        }

        for (library in icedLibraries) {
            val filesToCache = groupedDirtyFiles[library]?.let { libraryFiles ->
                val filesWithFqNames = libraryFilesWithFqNames[library]!!.associateBy {
                    CacheSupport.cacheFileId(it.fqName, it.filePath)
                }
                libraryFiles.map { filesWithFqNames[it.file]!!.filePath }
            }.orEmpty()

            when {
                library in needFullRebuild -> buildLibraryCache(library, false, emptyList())
                caches[library] == null || filesToCache.isNotEmpty() -> buildLibraryCache(library, false, filesToCache)
            }
        }
    }

    private fun buildLibraryCache(library: KotlinLibrary, isExternal: Boolean, filesToCache: List<String>) {
        val dependencies = library.getAllTransitiveDependencies(uniqueNameToLibrary)
        val dependencyCaches = dependencies.map {
            cacheRootDirectories[it] ?: run {
                configuration.report(CompilerMessageSeverity.LOGGING,
                        "SKIPPING ${library.location} as some of the dependencies aren't cached")
                return
            }
        }

        configuration.report(CompilerMessageSeverity.LOGGING, "CACHING ${library.location}")
        filesToCache.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "    $it") }

        // Produce monolithic caches for external libraries for now.
        val makePerFileCache = !isExternal && !library.isCInteropLibrary()

        val libraryCacheDirectory = when {
            library.isFromKotlinNativeDistribution || library.isNativeStdlib -> config.systemCacheDirectory
            isExternal -> CachedLibraries.computeLibraryCacheDirectory(
                    config.autoCacheDirectory, library, uniqueNameToLibrary, uniqueNameToHash)
            else -> config.incrementalCacheDirectory!!
        }
        val libraryCache = libraryCacheDirectory.child(
                if (makePerFileCache)
                    CachedLibraries.getPerFileCachedLibraryName(library)
                else
                    CachedLibraries.getCachedLibraryName(library)
        )
        libraryCacheDirectory.mkdirs()

        /*
         * Use lock file to not allow caches building in parallel. Actually, this is OK (there are some synchronization
         * mechanisms in the compiler) but may take up a lot of memory (especially when building stdlib cache). In particular,
         * this happens during some tests which specify certain binary options which won't allow to use the precompiled caches.
         */
        val lockFileName = "${libraryCache.absolutePath}.lock"
        val lockFile = java.io.File(lockFileName)
        // For now, per-file caches are only used for the incremental compilation which can't be run in parallel.
        val shouldUseLockFile = !makePerFileCache
        if (shouldUseLockFile) {
            // The lock file is intentionally kept so all contenders keep synchronizing on the same filesystem object.
            //
            //  If you delete the .lock file after releasing the lock, this race appears:
            //
            //  1. Process A holds a lock on cache.lock.
            //  2. Process B has already opened that file and is blocked waiting on the lock.
            //  3. Process A releases the lock and deletes cache.lock.
            //  4. Process C creates a new cache.lock path and locks that new file.
            //  5. Process B may still acquire the lock on the old, now-unlinked file it opened earlier.
            //
            //  Now B and C both think they own “the” cache lock, but they are locking different inodes. That breaks mutual exclusion.
            //
            //  Keeping the lock file avoids that class of bug because every process/thread always opens and locks the same persistent file. The lock state changes, but the lock target does not.
            //
            //  So the file is not kept because its contents matter. It is kept because the path must continue to refer to one stable lock object across owners.
            val locked = RandomAccessFile(lockFile, "rw").use { raf ->
                val lock = raf.channel.lockWithRetries(lockFile)
                try {
                    if (libraryCache.exists) {
                        cacheRootDirectories[library] = libraryCache.absolutePath
                        return@use true
                    }
                    tryBuildingLibraryCache(library, dependencies, dependencyCaches, libraryCacheDirectory, makePerFileCache, filesToCache, libraryCache)
                } finally {
                    lock.release()
                }
                false
            }
        } else {
            tryBuildingLibraryCache(library, dependencies, dependencyCaches, libraryCacheDirectory, makePerFileCache, filesToCache, libraryCache)
        }
    }

    private fun java.nio.channels.FileChannel.lockWithRetries(lockFile: java.io.File): FileLock {
        var retries = 0
        while (true) {
            try {
                return lock()
            } catch (_: OverlappingFileLockException) {
                Thread.sleep(lockRetrySleepMillis)
                retries++
                if (retries % 10 == 0) {
                    configuration.report(CompilerMessageSeverity.LOGGING, "Waiting to acquire lock: ${lockFile.path}")
                }
            }
        }
    }

    private fun tryBuildingLibraryCache(
            library: KotlinLibrary,
            dependencies: List<KotlinLibrary>,
            dependencyCaches: List<String>,
            libraryCacheDirectory: File,
            makePerFileCache: Boolean,
            filesToCache: List<String>,
            libraryCache: File,
    ) {
        try {
            // TODO: Run monolithic cache builds in parallel.
            spawnLibraryCacheBuild(library, dependencies, dependencyCaches, libraryCacheDirectory, makePerFileCache, filesToCache)
            cacheRootDirectories[library] = libraryCache.absolutePath
        } catch (t: Throwable) {
            try {
                libraryCache.deleteRecursively()
            } catch (_: Throwable) {
                // Nothing to do.
            }
            val message = (t as? CompilationErrorException)?.message
                    ?: run {
                        @Suppress("IncorrectFormatting") val extraUserInfo =
                                """
                                    Failed to build cache for ${library.location}.
                                    As a workaround, please try to disable ${
                                        if (makePerFileCache)
                                            "incremental compilation (kotlin.incremental.native=false)"
                                        else
                                            "compiler caches (https://kotl.in/disable-native-cache)"
                                    }

                                    Also, consider filing an issue with full Gradle log here: https://kotl.in/issue
                                    """.trimIndent()
                        "$extraUserInfo\n\n${t.message}\n\n${t.stackTraceToString()}"
                    }
            config.configuration.reportCompilationError(message)
        }
    }

    private fun spawnLibraryCacheBuild(
            library: KotlinLibrary,
            dependencies: List<KotlinLibrary>,
            dependencyCaches: List<String>,
            libraryCacheDirectory: File,
            makePerFileCache: Boolean,
            filesToCache: List<String>,
    ) {
        compilationSpawner.spawn(config.additionalCacheFlags /* TODO: Some way to put them directly to CompilerConfiguration? */) {
            val libraryPath = library.libraryFile.absolutePath
            val libraries = dependencies.filter { !it.isFromKotlinNativeDistribution }.map { it.libraryFile.absolutePath }
            val cachedLibraries = dependencies.zip(dependencyCaches).associate { it.first.libraryFile.absolutePath to it.second }
            configuration.report(CompilerMessageSeverity.LOGGING,
                    "-p static_cache -Xadd-cache=${library.location} \\\n" +
                            libraries.joinToString("\n") { "-library $it \\" } + "\n" +
                            cachedLibraries.entries.joinToString("\n") { "-Xcached-library=${it.key},${it.value} \\" } + "\n" +
                            "-Xcache-directory=${libraryCacheDirectory.absolutePath}\n"
            )

            setupCommonOptionsForCaches(config)
            konanProducedArtifactKind = CompilerOutputKind.STATIC_CACHE
            // CHECK_DEPENDENCIES is computed based on outputKind, which is overwritten in the line above
            // So we have to change CHECK_DEPENDENCIES accordingly, otherwise they might not be downloaded (see KT-67547)
            checkDependencies = true
            konanLibraryToAddToCache = libraryPath
            konanNoDefaultLibs = true
            konanNoEndorsedLibs = true
            konanNoStdlib = true
            konanLibraries = libraries
            val generateTestRunner = this@CacheBuilder.generateTestRunner
            if (generateTestRunner != TestRunnerKind.NONE && libraryPath in this@CacheBuilder.includedLibraries) {
                konanFriendLibraries = config.friendModuleFiles.map { it.absolutePath }
                this.generateTestRunner = generateTestRunner
                konanIncludedLibraries = listOf(libraryPath)
                configuration.testDumpOutputPath?.let { testDumpOutputPath = it }
            }
            this.cachedLibraries = cachedLibraries
            cacheDirectories = listOf(libraryCacheDirectory.absolutePath)
            this.makePerFileCache = makePerFileCache
            if (filesToCache.isNotEmpty())
                this.filesToCache = filesToCache
        }
    }
}
