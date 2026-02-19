/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.utils.SingleActionPerProject
import org.jetbrains.kotlin.gradle.utils.registerClassLoaderScopedBuildService
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit

internal interface UsesClassLoadersCachingBuildService : Task {
    @get:Internal
    val classLoadersCachingService: Property<ClassLoadersCachingBuildService>
}

/**
 * A [BuildService] for caching [ClassLoader] instances
 */
internal abstract class ClassLoadersCachingBuildService : BuildService<ClassLoadersCachingBuildService.Parameters> {
    internal interface Parameters : BuildServiceParameters {
        val classLoaderCacheTimeoutInSeconds: Property<Long>
    }

    private val logger = Logging.getLogger(javaClass)

    fun getClassLoader(
        classpath: List<File>,
        parentClassLoaderProvider: ParentClassLoaderProvider = DefaultParentClassLoaderProvider(),
    ): ClassLoader {
        val cache = ClassLoadersCacheHolder.getCache(parameters.classLoaderCacheTimeoutInSeconds.get())
        return cache.get(ClassLoaderCacheKey(classpath, parentClassLoaderProvider)) {
            logger.debug("Creating a new classloader for classpath $classpath")
            URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), parentClassLoaderProvider.getClassLoader())
        }
    }

    companion object {
        fun registerIfAbsent(project: Project): Provider<ClassLoadersCachingBuildService> =
            project.gradle.registerClassLoaderScopedBuildService(ClassLoadersCachingBuildService::class) {
                it.parameters.classLoaderCacheTimeoutInSeconds.set(project.kotlinPropertiesProvider.classLoaderCacheTimeoutInSeconds)
            }.also { serviceProvider ->
                SingleActionPerProject.run(project, UsesClassLoadersCachingBuildService::class.java.name) {
                    project.tasks.withType<UsesClassLoadersCachingBuildService>().configureEach { task ->
                        task.usesService(serviceProvider)
                        task.classLoadersCachingService.value(serviceProvider).disallowChanges()
                    }
                }
            }
    }
}

/**
 * Object holder for a thread-safe cache of class loaders.
 * Persists the cache between builds using the same daemon unless Gradle has changed the class loader used to load KGP.
 * In such cases, the remaining cache is garbage collected with the related class loader.
 *
 * The cache utilizes a combination of time-based eviction and soft references for efficient resource usage.
 *
 * Implementation as a separate object is chosen over a [ClassLoadersCachingBuildService] companion object because:
 * 1. Cache instantiation requires a [Project] instance, which may not be available before [ClassLoadersCachingBuildService.registerIfAbsent] is called.
 * 2. This design ensures the implementation is configuration cache-safe, considering the case of cold daemon run with deserialized state.
 */
private object ClassLoadersCacheHolder {
    /**
     * The service can be accessed by multiple tasks concurrently, hence the cache must be thread-safe.
     * As per `com.google.common.cache.Cache` documentation, implementations are expected to be thread-safe.
     * Utilizes the double-check locking singleton pattern, so @Volatile is crucial for correctness.
     */
    @Volatile
    private lateinit var classLoaders: Cache<ClassLoaderCacheKey, ClassLoader>

    /**
     * [classLoaderCacheTimeoutInSeconds] is used only for the cache initialization and may not be changed after initialization.
     *
     * @param classLoaderCacheTimeoutInSeconds the duration in seconds after which a cache entry will expire if not accessed
     */
    fun getCache(classLoaderCacheTimeoutInSeconds: Long): Cache<ClassLoaderCacheKey, ClassLoader> {
        if (!::classLoaders.isInitialized) {
            synchronized(this) {
                if (!::classLoaders.isInitialized) {
                    classLoaders = CacheBuilder.newBuilder()
                        .expireAfterAccess(classLoaderCacheTimeoutInSeconds, TimeUnit.SECONDS)
                        .softValues()
                        .build<ClassLoaderCacheKey, ClassLoader>()
                }
            }
        }
        return classLoaders
    }
}

private data class ClassLoaderCacheKey(
    val classpath: List<File>,
    val parentClassLoaderProvider: ParentClassLoaderProvider,
)

/**
 * A provider of the parent [ClassLoader] for a newly created ClassLoader instances.
 *
 * An implementation must override `equals` and `hashCode`! It's used as a part of a [Map] key
 */
internal fun interface ParentClassLoaderProvider {
    fun getClassLoader(): ClassLoader?
}

private class DefaultParentClassLoaderProvider : ParentClassLoaderProvider {
    override fun getClassLoader(): ClassLoader = javaClass.classLoader

    override fun hashCode() = javaClass.hashCode()

    override fun equals(other: Any?) = other is DefaultParentClassLoaderProvider && other.javaClass == javaClass
}