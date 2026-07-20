/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.cache

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal val Project.kotlinGradleTaskExecutionCache: Provider<KotlinGradleTaskExecutionCache>
    get() = gradle.registerClassLoaderScopedBuildService(KotlinGradleTaskExecutionCache::class)

/**
 * Build service that can be used to memoize computation results between different task instances build-wide.
 * i.e. all subprojects can store and access shared instances in this storage service.
 */
internal abstract class KotlinGradleTaskExecutionCache : BuildService<BuildServiceParameters.None> {
    private val extras = mutableExtrasOf()

    operator fun <V> contains(key: Extras.Key<V>): Boolean = extras.contains(key)
    operator fun <V> set(key: Extras.Key<V>, value: V): V? = extras.set(key, value)
    operator fun <V> get(key: Extras.Key<V>): V? = extras[key]
}

@Suppress("UNCHECKED_CAST")
internal inline fun <V> KotlinGradleTaskExecutionCache.getOrPutSynchronized(
    key: Extras.Key<V>,
    compute: () -> V
): V {
    if (contains(key)) return get(key) as V
    synchronized(this) {
        if (contains(key)) return get(key) as V
        val value = compute()
        set(key, value)
        return value
    }
}
