/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistry
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

@JvmInline
private value class IsolatedClassLoaderCacheKey private constructor(private val classpath: String) {
    constructor(classpath: Set<File>) : this(classpath.map { it.absolutePath }.sorted().joinToString(separator = File.pathSeparator))
}

abstract class KonanCliRunnerIsolatedClassLoadersService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val isolatedClassLoaders = ConcurrentHashMap<IsolatedClassLoaderCacheKey, URLClassLoader>()

    override fun close() {
        isolatedClassLoaders.clear()
    }

    /**
     * Get a [ClassLoader] for the given [classpath].
     *
     * During a single build, this will attempt to reuse class loaders for the same [classpath].
     */
    fun getClassLoader(classpath: Set<File>): ClassLoader = isolatedClassLoaders.computeIfAbsent(IsolatedClassLoaderCacheKey(classpath)) {
        val arrayOfURLs = classpath.map { File(it.absolutePath).toURI().toURL() }.toTypedArray()
        URLClassLoader(arrayOfURLs, null).apply {
            setDefaultAssertionStatus(true)
        }
    }
}

fun BuildServiceRegistry.registerIsolatedClassLoadersServiceIfAbsent(): Provider<KonanCliRunnerIsolatedClassLoadersService> =
        registerIfAbsent("KonanCliRunnerIsolatedClassLoadersService", KonanCliRunnerIsolatedClassLoadersService::class.java) {}