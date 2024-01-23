/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.serialization.FingerprintHash
import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.library.metadata.isInteropLibrary

internal fun KotlinLibrary.getAllTransitiveDependencies(allLibraries: Map<String, KotlinLibrary>): List<KotlinLibrary> {
    val allDependencies = mutableSetOf<KotlinLibrary>()

    fun traverseDependencies(library: KotlinLibrary) {
        library.unresolvedDependencies.forEach {
            val dependency = allLibraries[it.path]!!
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
        val konanConfig: KonanConfig,
        val compilationSpawner: CompilationSpawner
) {
    private val configuration = konanConfig.configuration
    private val autoCacheableFrom = configuration.get(KonanConfigKeys.AUTO_CACHEABLE_FROM)!!.map { File(it) }
    private val icEnabled = configuration.get(CommonConfigurationKeys.INCREMENTAL_COMPILATION)!!
    private val includedLibraries = configuration.get(KonanConfigKeys.INCLUDED_LIBRARIES).orEmpty().toSet()
    private val generateTestRunner = configuration.getNotNull(KonanConfigKeys.GENERATE_TEST_RUNNER)

    fun needToBuild() = konanConfig.isFinalBinary && konanConfig.ignoreCacheReason == null && (autoCacheableFrom.isNotEmpty() || icEnabled)

    private val allLibraries by lazy { konanConfig.resolvedLibraries.getFullList(TopologicalLibraryOrder) }
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
        get() = autoCacheableFrom.any { libraryFile.absolutePath.startsWith(it.absolutePath) }

    fun build() {
        val externalLibrariesToCache = mutableListOf<KotlinLibrary>()
        val icedLibraries = mutableListOf<KotlinLibrary>()

        val stdlib = konanConfig.distribution.stdlib
        allLibraries.forEach { library ->
            val isSubjectOfIC = !library.isDefault && !library.isExternal && !library.libraryName.startsWith(stdlib)
            val cache = konanConfig.cachedLibraries.getLibraryCache(library, allowIncomplete = isSubjectOfIC)
            cache?.let {
                caches[library] = it
                cacheRootDirectories[library] = it.rootDirectory
            }
            if (isSubjectOfIC) {
                icedLibraries += library
            } else {
                if (cache == null) externalLibrariesToCache += library
            }
            library.unresolvedDependencies.forEach {
                val dependency = uniqueNameToLibrary[it.path]!!
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
                require(library.isInteropLibrary())
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
                    val previousContentHash = FingerprintHash.fromByteArray(cache.getFileHash(cachedFile))
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
                                kind.files.forEach {
                                    reversedPerFileDependencies.getOrPut(LibraryFile(dependentLibrary, it)) { mutableListOf() }.add(libraryFile)
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
        icedLibraries.filter { caches[it] != null }.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        ${it.libraryName}") }
        configuration.report(CompilerMessageSeverity.LOGGING, "    CLEAN BUILD:")
        icedLibraries.filter { caches[it] == null }.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        ${it.libraryName}") }
        configuration.report(CompilerMessageSeverity.LOGGING, "    FULL REBUILD:")
        icedLibraries.filter { it in needFullRebuild }.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "        ${it.libraryName}") }
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
                        "SKIPPING ${library.libraryName} as some of the dependencies aren't cached")
                return
            }
        }

        configuration.report(CompilerMessageSeverity.LOGGING, "CACHING ${library.libraryName}")
        filesToCache.forEach { configuration.report(CompilerMessageSeverity.LOGGING, "    $it") }

        // Produce monolithic caches for external libraries for now.
        val makePerFileCache = !isExternal && !library.isInteropLibrary()

        val libraryCacheDirectory = when {
            library.isDefault -> konanConfig.systemCacheDirectory
            isExternal -> CachedLibraries.computeVersionedCacheDirectory(
                    konanConfig.autoCacheDirectory, library, uniqueNameToLibrary, uniqueNameToHash)
            else -> konanConfig.incrementalCacheDirectory!!
        }
        val libraryCache = libraryCacheDirectory.child(
                if (makePerFileCache)
                    CachedLibraries.getPerFileCachedLibraryName(library)
                else
                    CachedLibraries.getCachedLibraryName(library)
        )
        try {
            // TODO: Run monolithic cache builds in parallel.
            libraryCacheDirectory.mkdirs()
            compilationSpawner.spawn(konanConfig.additionalCacheFlags /* TODO: Some way to put them directly to CompilerConfiguration? */) {
                val libraryPath = library.libraryFile.absolutePath
                val libraries = dependencies.filter { !it.isDefault }.map { it.libraryFile.absolutePath }
                val cachedLibraries = dependencies.zip(dependencyCaches).associate { it.first.libraryFile.absolutePath to it.second }
                configuration.report(CompilerMessageSeverity.LOGGING, "    dependencies:\n        " +
                        libraries.joinToString("\n        "))
                configuration.report(CompilerMessageSeverity.LOGGING, "    caches used:\n        " +
                        cachedLibraries.entries.joinToString("\n        ") { "${it.key}: ${it.value}" })
                configuration.report(CompilerMessageSeverity.LOGGING, "    cache dir: " +
                        libraryCacheDirectory.absolutePath)

                setupCommonOptionsForCaches(konanConfig)
                put(KonanConfigKeys.PRODUCE, CompilerOutputKind.STATIC_CACHE)
                put(KonanConfigKeys.LIBRARY_TO_ADD_TO_CACHE, libraryPath)
                put(KonanConfigKeys.NODEFAULTLIBS, true)
                put(KonanConfigKeys.NOENDORSEDLIBS, true)
                put(KonanConfigKeys.NOSTDLIB, true)
                put(KonanConfigKeys.LIBRARY_FILES, libraries)
                if (generateTestRunner != TestRunnerKind.NONE && libraryPath in includedLibraries) {
                    put(KonanConfigKeys.GENERATE_TEST_RUNNER, generateTestRunner)
                    put(KonanConfigKeys.INCLUDED_LIBRARIES, listOf(libraryPath))
                    configuration.get(KonanConfigKeys.TEST_DUMP_OUTPUT_PATH)?.let { put(KonanConfigKeys.TEST_DUMP_OUTPUT_PATH, it) }
                }
                put(KonanConfigKeys.CACHED_LIBRARIES, cachedLibraries)
                put(KonanConfigKeys.CACHE_DIRECTORIES, listOf(libraryCacheDirectory.absolutePath))
                put(KonanConfigKeys.MAKE_PER_FILE_CACHE, makePerFileCache)
                if (filesToCache.isNotEmpty())
                    put(KonanConfigKeys.FILES_TO_CACHE, filesToCache)
            }
            cacheRootDirectories[library] = libraryCache.absolutePath
        } catch (t: Throwable) {
            configuration.report(CompilerMessageSeverity.LOGGING, "${t.message}\n${t.stackTraceToString()}")
            configuration.report(CompilerMessageSeverity.WARNING,
                    "Failed to build cache: ${t.message}\n${t.stackTraceToString()}\n" +
                            "Falling back to not use cache for ${library.libraryName}")

            libraryCache.deleteRecursively()
        }
    }
}