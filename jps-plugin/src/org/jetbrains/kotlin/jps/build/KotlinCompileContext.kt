/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.GlobalContextKey
import org.jetbrains.jps.incremental.fs.CompilationRound
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.storage.version.CacheAttributesDiff
import org.jetbrains.kotlin.incremental.storage.version.CacheStatus
import org.jetbrains.kotlin.incremental.storage.version.loadDiff
import org.jetbrains.kotlin.jps.incremental.*
import org.jetbrains.kotlin.jps.targets.KotlinTargetsIndex
import org.jetbrains.kotlin.jps.targets.KotlinTargetsIndexBuilder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  KotlinCompileContext is shared between all threads (i.e. it is [GlobalContextKey]).
 *
 *  It is initialized lazily, and only before building of first chunk with kotlin code,
 *  and will be disposed on build finish.
 */
internal val CompileContext.kotlin: KotlinCompileContext
    get() {
        val userData = getUserData(kotlinCompileContextKey)
        if (userData != null) return userData

        // here is error (KotlinCompilation available only at build phase)
        // let's also check for concurrent initialization
        val errorMessage = "KotlinCompileContext available only at build phase " +
                "(between first KotlinBuilder.chunkBuildStarted and KotlinBuilder.buildFinished)"

        synchronized(kotlinCompileContextKey) {
            val newUsedData = getUserData(kotlinCompileContextKey)
            if (newUsedData != null) {
                error("Concurrent CompileContext.kotlin getter call and KotlinCompileContext initialization detected: $errorMessage")
            }
        }

        error(errorMessage)
    }

internal val kotlinCompileContextKey = GlobalContextKey<KotlinCompileContext>("kotlin")

class KotlinCompileContext(val jpsContext: CompileContext) {
    val dataManager = jpsContext.projectDescriptor.dataManager
    val dataPaths = dataManager.dataPaths
    val testingLogger: TestingBuildLogger?
        get() = jpsContext.testingContext?.buildLogger

    val targetsIndex: KotlinTargetsIndex = KotlinTargetsIndexBuilder(this).build()

    val targetsBinding
        get() = targetsIndex.byJpsTarget

    val lookupsCacheAttributesManager: CompositeLookupsCacheAttributesManager = makeLookupsCacheAttributesManager()

    val initialLookupsCacheStateDiff: CacheAttributesDiff<*> = loadLookupsCacheStateDiff()

    val shouldCheckCacheVersions = System.getProperty(KotlinBuilder.SKIP_CACHE_VERSION_CHECK_PROPERTY) == null

    val hasKotlinMarker = HasKotlinMarker(dataManager)

    /**
     * Flag to prevent rebuilding twice.
     *
     * TODO: looks like it is not required since cache version checking are refactored
     */
    val rebuildAfterCacheVersionChanged = RebuildAfterCacheVersionChangeMarker(dataManager)

    var rebuildingAllKotlin = false

    private fun makeLookupsCacheAttributesManager(): CompositeLookupsCacheAttributesManager {
        val expectedLookupsCacheComponents = mutableSetOf<String>()

        targetsIndex.chunks.forEach { chunk ->
            chunk.targets.forEach { target ->
                if (target.isIncrementalCompilationEnabled) {
                    expectedLookupsCacheComponents.add(target.globalLookupCacheId)
                }
            }
        }

        val lookupsCacheRootPath = dataPaths.getTargetDataRoot(KotlinDataContainerTarget)
        return CompositeLookupsCacheAttributesManager(lookupsCacheRootPath, expectedLookupsCacheComponents)
    }

    private fun loadLookupsCacheStateDiff(): CacheAttributesDiff<CompositeLookupsCacheAttributes> {
        val diff = lookupsCacheAttributesManager.loadDiff()

        if (diff.status == CacheStatus.VALID) {
            // try to perform a lookup
            // request rebuild if storage is corrupted
            try {
                dataManager.withLookupStorage {
                    it.get(LookupSymbol("<#NAME#>", "<#SCOPE#>"))
                }
            } catch (e: Exception) {
                jpsReportInternalBuilderError(jpsContext, Error("Lookup storage is corrupted, probe failed: ${e.message}", e))
                markAllKotlinForRebuild("Lookup storage is corrupted")
                return diff.copy(actual = null)
            }
        }

        return diff
    }

    fun hasKotlin() = targetsIndex.chunks.any { chunk ->
        chunk.targets.any { target ->
            hasKotlinMarker[target] == true
        }
    }

    fun checkCacheVersions() {
        when (initialLookupsCacheStateDiff.status) {
            CacheStatus.INVALID -> {
                // global cache needs to be rebuilt

                testingLogger?.invalidOrUnusedCache(null, null, initialLookupsCacheStateDiff)

                if (initialLookupsCacheStateDiff.actual != null) {
                    markAllKotlinForRebuild("Kotlin incremental cache settings or format was changed")
                    clearLookupCache()
                } else {
                    markAllKotlinForRebuild("Kotlin incremental cache is missed or corrupted")
                }
            }
            CacheStatus.VALID -> Unit
            CacheStatus.SHOULD_BE_CLEARED -> {
                jpsContext.testingContext?.buildLogger?.invalidOrUnusedCache(null, null, initialLookupsCacheStateDiff)
                KotlinBuilder.LOG.info("Removing global cache as it is not required anymore: $initialLookupsCacheStateDiff")

                clearAllCaches()
            }
            CacheStatus.CLEARED -> Unit
        }
    }

    private val lookupAttributesSaved = AtomicBoolean(false)

    /**
     * Called on every successful compilation
     */
    fun ensureLookupsCacheAttributesSaved() {
        if (lookupAttributesSaved.compareAndSet(false, true)) {
            initialLookupsCacheStateDiff.saveExpectedIfNeeded()
        }
    }

    fun checkChunkCacheVersion(chunk: KotlinChunk) {
        if (shouldCheckCacheVersions && !rebuildingAllKotlin) {
            if (chunk.shouldRebuild()) markChunkForRebuildBeforeBuild(chunk)
        }
    }

    private fun logMarkDirtyForTestingBeforeRound(file: File, shouldProcess: Boolean): Boolean {
        if (shouldProcess) {
            testingLogger?.markedAsDirtyBeforeRound(listOf(file))
        }
        return shouldProcess
    }

    private fun markAllKotlinForRebuild(reason: String) {
        if (rebuildingAllKotlin) return
        rebuildingAllKotlin = true

        KotlinBuilder.LOG.info("Rebuilding all Kotlin: $reason")

        val dataManager = jpsContext.projectDescriptor.dataManager

        targetsIndex.chunks.forEach {
            markChunkForRebuildBeforeBuild(it)
        }

        dataManager.cleanLookupStorage(KotlinBuilder.LOG)
    }

    private fun markChunkForRebuildBeforeBuild(chunk: KotlinChunk) {
        chunk.targets.forEach {
            FSOperations.markDirty(jpsContext, CompilationRound.NEXT, it.jpsModuleBuildTarget) { file ->
                logMarkDirtyForTestingBeforeRound(file, file.isKotlinSourceFile)
            }

            dataManager.getKotlinCache(it)?.clean()
            hasKotlinMarker.clean(it)
            rebuildAfterCacheVersionChanged[it] = true
        }
    }

    private fun clearAllCaches() {
        clearLookupCache()

        KotlinBuilder.LOG.info("Clearing caches for all targets")
        targetsIndex.chunks.forEach { chunk ->
            chunk.targets.forEach {
                dataManager.getKotlinCache(it)?.clean()
            }
        }
    }

    private fun clearLookupCache() {
        KotlinBuilder.LOG.info("Clearing lookup cache")
        dataManager.cleanLookupStorage(KotlinBuilder.LOG)
        initialLookupsCacheStateDiff.saveExpectedIfNeeded()
    }

    fun cleanupCaches() {
        // todo: remove lookups for targets with disabled IC (or split global lookups cache into several caches for each compiler)

        targetsIndex.chunks.forEach { chunk ->
            chunk.targets.forEach { target ->
                if (target.initialLocalCacheAttributesDiff.status == CacheStatus.SHOULD_BE_CLEARED) {
                    KotlinBuilder.LOG.info(
                        "$target caches is cleared as not required anymore: ${target.initialLocalCacheAttributesDiff}"
                    )
                    testingLogger?.invalidOrUnusedCache(null, target, target.initialLocalCacheAttributesDiff)
                    dataManager.getKotlinCache(target)?.clean()
                }
            }
        }
    }

    fun dispose() {

    }

    fun getChunk(rawChunk: ModuleChunk): KotlinChunk? {
        val rawRepresentativeTarget = rawChunk.representativeTarget()
        if (rawRepresentativeTarget !in targetsBinding) return null

        return targetsIndex.chunksByJpsRepresentativeTarget[rawRepresentativeTarget]
            ?: error("Kotlin binding for chunk $this is not loaded at build start")
    }
}