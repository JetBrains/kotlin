/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.cache

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/**
 * A [KotlinGradleTaskExecutionCache] that additionally measures how long every entry took to compute
 * and how many times it was reused afterwards.
 *
 * On [close] the per-entry metrics are written into [Parameters.metricsOutputFile] as a headerless CSV
 * with one `key,hits,computationTimeNanos` row per cache entry, and a summary is logged.
 */
internal abstract class KotlinGradleTaskExecutionCacheWithMetrics :
    KotlinGradleTaskExecutionCache,
    BuildService<KotlinGradleTaskExecutionCacheWithMetrics.Parameters>,
    AutoCloseable {

    internal interface Parameters : BuildServiceParameters {
        val metricsOutputFile: RegularFileProperty
    }

    private val storage = KotlinConcurrentGetOrComputeStorage()
    private val logger = Logging.getLogger(KotlinGradleTaskExecutionCacheWithMetrics::class.java)

    private class CacheEntry<V>(
        val value: V,
        val computationTime: Duration,
    ) {
        /** Starts at -1, so the very access that computed the value is not counted as a hit. */
        private val accesses = AtomicInteger(-1)
        fun registerAccess() = accesses.incrementAndGet()
        val hits: Int get() = accesses.get()

        val savedTime: Duration get() = computationTime * hits
    }

    @OptIn(ExperimentalTime::class)
    override fun <V> getOrCompute(
        key: String,
        compute: () -> V
    ): V {
        val cacheEntry = storage.getOrCompute(key) {
            val (value, computationTime) = measureTimedValue(compute)
            CacheEntry(value, computationTime)
        }
        cacheEntry.registerAccess()
        return cacheEntry.value
    }

    override fun close() {
        val entries = storage.snapshotCacheEntries()
            .map { (key, value) -> key to value as CacheEntry<*> }
            .sortedByDescending { (_, entry) -> entry.savedTime }

        val metricsOutputFile = parameters.metricsOutputFile.asFile.get()
        metricsOutputFile.parentFile?.mkdirs()
        metricsOutputFile.writeText(
            buildString {
                for ((key, entry) in entries) {
                    appendLine("$key,${entry.hits},${entry.computationTime.inWholeNanoseconds}")
                }
            }
        )

        logger.lifecycle(
            buildString {
                appendLine("Kotlin Gradle task execution cache:")
                appendLine("  Total entries: ${entries.size}")
                appendLine("  Total hits: ${entries.sumOf { (_, entry) -> entry.hits }}")
                appendLine("  Entries never reused: ${entries.count { (_, entry) -> entry.hits == 0 }}")
                appendLine("  Sum of saved times: ${entries.fold(0.seconds) { acc, (_, entry) -> acc + entry.savedTime }}")
                appendLine("  Per-entry metrics: $metricsOutputFile")
            }
        )
    }
}
