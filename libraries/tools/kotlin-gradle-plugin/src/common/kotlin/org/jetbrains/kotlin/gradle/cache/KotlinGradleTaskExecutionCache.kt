/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.cache

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

internal val Project.kotlinGradleTaskExecutionCache: Provider<out KotlinGradleTaskExecutionCache>
    get() {
        val metricsFile = kotlinPropertiesProvider.taskExecutionCacheMetricsFile
        return if (metricsFile == null) {
            gradle.registerClassLoaderScopedBuildService(DefaultKotlinGradleTaskExecutionCache::class)
        } else {
            gradle.registerClassLoaderScopedBuildService(KotlinGradleTaskExecutionCacheWithMetrics::class) {
                it.parameters.metricsOutputFile.set(rootDir.resolve(metricsFile))
            }
        }
    }

/**
 * Cache that can be used to memoize computation results between different task instances build-wide.
 * i.e. all subprojects can store and access shared instances in this storage service.
 */
internal interface KotlinGradleTaskExecutionCache {
    fun <V> getOrCompute(
        key: String,
        compute: () -> V
    ): V
}

/**
 * Default [KotlinGradleTaskExecutionCache] implementation: a plain build service around
 * [KotlinConcurrentGetOrComputeStorage] with no bookkeeping on top of it.
 */
internal abstract class DefaultKotlinGradleTaskExecutionCache :
    KotlinGradleTaskExecutionCache,
    BuildService<BuildServiceParameters.None> {
    private val storage = KotlinConcurrentGetOrComputeStorage()

    override fun <V> getOrCompute(
        key: String,
        compute: () -> V
    ): V = storage.getOrCompute(key, compute)
}

/**
 * Flag to prevent from double entries, even with different keys.
 */
private val computingOnThread = ThreadLocal.withInitial { false }

/**
 * Should be used only in [KotlinGradleTaskExecutionCache], extracted to separate class for testing needs
 */
internal class KotlinConcurrentGetOrComputeStorage {
    private val hashMap = ConcurrentHashMap<String, FutureTask<*>>()

    /**
     * Returns the values computed so far, for reporting purposes.
     *
     * Entries whose computation has failed are skipped: the failure is already propagated to whoever
     * requested the value, and reporting must not fail the build on top of that.
     */
    fun snapshotCacheEntries(): Map<String, Any?> = buildMap {
        hashMap.forEach { (key, task) ->
            val value = try {
                task.get()
            } catch (_: ExecutionException) {
                return@forEach
            }
            put(key, value)
        }
    }

    /**
     * Gets existing value by [key] or computes new one using [compute].
     * Computation will happen once. If it failed, the failure will be stored forever.
     * This is expected and desired behavior for the [KotlinGradleTaskExecutionCache] usecases.
     *
     * TL;DR: [compute] should be idempotent and return the same result for the same [key].
     *
     * **Warning** double entry is not accepted!
     * Avoid writing code like this:
     * ```kotlin
     * getOrCompute(key) {
     *   getOrCompute(key) { // or even different key
     *     // computation
     *   }
     * }
     * ```
     */
    fun <V> getOrCompute(
        key: String,
        compute: () -> V
    ): V {
        /** implementation note:
         * It is theoretically possible to use [ConcurrentHashMap.computeIfAbsent]
         * but, according to its Javadoc, when [compute] is too slow, it will cause unnecessary blocks even for different keys.
         * */

        check(!computingOnThread.get()) { "Double entry is not accepted!" }
        var existingTask = hashMap[key]
        val task = if (existingTask != null) {
            // Round 1: try getting fast
            existingTask
        } else {
            // Round 2: allocate newTask and try to put it
            val newTask = FutureTask(compute)
            existingTask = hashMap.putIfAbsent(key, newTask)
            if (existingTask == null) {
                // Round 3: we're the first one, compute the value, and let other threads to wait
                computingOnThread.set(true)
                try {
                    newTask.run()
                } finally {
                    computingOnThread.set(false)
                }
                newTask
            } else existingTask
        }

        try {
            // blocks if necessary to await completion by other thread
            @Suppress("UNCHECKED_CAST")
            return task.get() as V
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }
}
