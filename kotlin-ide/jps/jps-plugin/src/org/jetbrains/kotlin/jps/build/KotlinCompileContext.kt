/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.GlobalContextKey
import org.jetbrains.jps.incremental.fs.CompilationRound
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.incremental.messages.CompilerMessage
import org.jetbrains.kotlin.config.CompilerRunnerConstants
import org.jetbrains.kotlin.config.CompilerRunnerConstants.*
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.jps.incremental.*
import org.jetbrains.kotlin.jps.targets.KotlinTargetsIndex
import org.jetbrains.kotlin.jps.targets.KotlinTargetsIndexBuilder
import org.jetbrains.kotlin.jps.targets.KotlinUnsupportedModuleBuildTarget
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

    val shouldCheckCacheVersions = System.getProperty(KotlinBuilder.SKIP_CACHE_VERSION_CHECK_PROPERTY) == null

    val hasKotlinMarker = HasKotlinMarker(dataManager)

    val isInstrumentationEnabled: Boolean by lazy {
        val value = System.getProperty("kotlin.jps.instrument.bytecode")?.toBoolean() ?: false
        if (value) {
            val message = "Experimental bytecode instrumentation for Kotlin classes is enabled"
            jpsContext.processMessage(CompilerMessage(KOTLIN_COMPILER_NAME, BuildMessage.Kind.INFO, message))
        }
        value
    }

    val fileToPathConverter: FileToPathConverter =
        JpsFileToPathConverter(jpsContext.projectDescriptor.project)

    val lookupStorageManager = JpsLookupStorageManager(dataManager, fileToPathConverter)

    /**
     * Flag to prevent rebuilding twice.
     *
     * TODO: looks like it is not required since cache version checking are refactored
     */
    val rebuildAfterCacheVersionChanged = RebuildAfterCacheVersionChangeMarker(dataManager)

    var rebuildingAllKotlin = false

    /**
     * Note, [loadLookupsCacheStateDiff] should be initialized last as it requires initialized
     * [targetsIndex], [hasKotlinMarker] and [rebuildAfterCacheVersionChanged] (see [markChunkForRebuildBeforeBuild])
     */
    private val initialLookupsCacheStateDiff: CacheAttributesDiff<*> = loadLookupsCacheStateDiff()

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
                lookupStorageManager.withLookupStorage {
                    it.get(LookupSymbol("<#NAME#>", "<#SCOPE#>"))
                }
            } catch (e: Exception) {
                // replace to jpsReportInternalBuilderError when IDEA-201297 will be implemented
                jpsContext.processMessage(
                    CompilerMessage(
                        "Kotlin", BuildMessage.Kind.WARNING,
                        "Incremental caches are corrupted. All Kotlin code will be rebuilt."
                    )
                )
                KotlinBuilder.LOG.info(Error("Lookup storage is corrupted, probe failed: ${e.message}", e))

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
            initialLookupsCacheStateDiff.manager.writeVersion()
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

        targetsIndex.chunks.forEach {
            markChunkForRebuildBeforeBuild(it)
        }

        lookupStorageManager.cleanLookupStorage(KotlinBuilder.LOG)
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
        lookupStorageManager.cleanLookupStorage(KotlinBuilder.LOG)
        initialLookupsCacheStateDiff.manager.writeVersion()
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
                    target.initialLocalCacheAttributesDiff.manager.writeVersion(null)
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

    fun reportUnsupportedTargets() {
        // group all KotlinUnsupportedModuleBuildTarget by kind
        // only representativeTarget will be added
        val byKind = mutableMapOf<String?, MutableList<KotlinUnsupportedModuleBuildTarget>>()

        targetsIndex.chunks.forEach {
            val target = it.representativeTarget
            if (target is KotlinUnsupportedModuleBuildTarget) {
                if (target.sourceFiles.isNotEmpty()) {
                    byKind.getOrPut(target.kind) { mutableListOf() }.add(target)
                }
            }
        }

        byKind.forEach { (kind, targets) ->
            targets.sortBy { it.module.name }
            val chunkNames = targets.map { it.chunk.presentableShortName }
            val presentableChunksListString = chunkNames.joinToReadableString()

            val msg =
                if (kind == null) {
                    "$presentableChunksListString is not yet supported in IDEA internal build system. " +
                            "Please use Gradle to build them (enable 'Delegate IDE build/run actions to Gradle' in Settings)."
                } else {
                    "$kind is not yet supported in IDEA internal build system. " +
                            "Please use Gradle to build $presentableChunksListString (enable 'Delegate IDE build/run actions to Gradle' in Settings)."
                }

            testingLogger?.addCustomMessage(msg)
            jpsContext.processMessage(
                CompilerMessage(
                    KOTLIN_COMPILER_NAME,
                    BuildMessage.Kind.WARNING,
                    msg
                )
            )
        }
    }
}

fun List<String>.joinToReadableString(): String = when {
    size > 5 -> take(5).joinToString() + " and ${size - 5} more"
    size > 1 -> dropLast(1).joinToString() + " and ${last()}"
    size == 1 -> single()
    else -> ""
}