package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.modules.TargetId
import java.io.File

internal class IncrementalCachesManager (
        private val targetId: TargetId,
        private val cacheDirectory: File,
        private val outputDir: File
) {
    private val incrementalCacheDir = File(cacheDirectory, "increCache.${targetId.name}")
    private val lookupCacheDir = File(cacheDirectory, "lookups")
    private var incrementalCacheOpen = false
    private var lookupCacheOpen = false

    val incrementalCache: GradleIncrementalCacheImpl by lazy {
        val cache = GradleIncrementalCacheImpl(targetDataRoot = incrementalCacheDir.apply { mkdirs() }, targetOutputDir = outputDir, target = targetId)
        incrementalCacheOpen = true
        cache
    }

    val lookupCache: LookupStorage by lazy {
        val cache = LookupStorage(lookupCacheDir.apply { mkdirs() })
        lookupCacheOpen = true
        cache
    }

    fun clean() {
        close(flush = false)
        cacheDirectory.deleteRecursively()
    }

    fun close(flush: Boolean = false) {
        if (incrementalCacheOpen) {
            if (flush) {
                incrementalCache.flush(false)
            }
            incrementalCache.close()
            incrementalCacheOpen = false
        }

        if (lookupCacheOpen) {
            if (flush) {
                lookupCache.flush(false)
            }
            lookupCache.close()
            lookupCacheOpen = false
        }
    }
}