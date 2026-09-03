/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.classloaders

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.jetbrains.kotlin.gradle.internal.KaptExecutionToken
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * LRU cache for [ClassLoader]s by class path.
 */
class ClassLoadersCache(
    size: Int,
    private val parentClassLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
    ttl: Duration = Duration.ofHours(1),
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(ClassLoadersCache::class.java)

    private val cache: Cache<CacheKey, URLClassLoader> =
        CacheBuilder
            .newBuilder()
            .maximumSize(size.toLong())
            .expireAfterAccess(ttl)
            .removalListener<CacheKey, URLClassLoader> { (key, cl) ->
                check(key != null && cl != null)
                logger.info("Removing classloader from cache: ${key.entries.map { it.path }}")
                cl.close()
            }
            .build()

    private val executionLocalLoaders = ConcurrentHashMap<KaptExecutionToken, MutableList<URLClassLoader>>()

    fun getForClassPath(files: List<File>): ClassLoader = getForClassPath(files, parentClassLoader)

    private fun getForClassPath(files: List<File>, parent: ClassLoader): ClassLoader {
        val key = makeKey(files)
        val classLoader = cache.asMap().computeIfAbsent(key) {
            makeClassLoader(files, parent)
        }
        // Guava delivers removal notifications during subsequent cache operations. This cache is
        // touched about once per kapt task, so without an explicit cleanUp an evicted loader can go
        // unclosed - and keep its jars open - indefinitely.
        cache.cleanUp()
        return classLoader
    }

    /**
     * Gets a [ClassLoader] for annotation processor classpath split by its retention lifecycle.
     *
     * [cacheableProcessorClasspath] gets a reusable loader. [executionLocalProcessorClasspath] gets
     * a per-execution loader that must be released with [releaseExecutionLocalLoaders] once annotation
     * processing has finished.
     *
     * This is useful when project-local annotation processors may depend on reusable processor
     * artifacts, but reusable processors must not depend on project-local artifacts.
     */
    internal fun getForProcessorClasspath(
        executionToken: KaptExecutionToken,
        executionLocalProcessorClasspath: List<File>,
        cacheableProcessorClasspath: List<File>,
    ): ClassLoader {
        val executionLoaders = synchronized(executionToken) {
            check(!executionToken.closed) { "ClassLoadersCache execution is already closed" }
            executionLocalLoaders.computeIfAbsent(executionToken) { mutableListOf() }
        }

        val parent = if (cacheableProcessorClasspath.isEmpty()) parentClassLoader else getForClassPath(cacheableProcessorClasspath)
        if (executionLocalProcessorClasspath.isEmpty()) return parent

        val local = makeClassLoader(executionLocalProcessorClasspath, parent)
        synchronized(executionToken) {
            if (executionToken.closed) {
                local.close()
                error("ClassLoadersCache execution is already closed")
            }
            executionLoaders.add(local)
        }
        return local
    }

    internal fun releaseExecutionLocalLoaders(executionToken: KaptExecutionToken) {
        val executionLoaders = synchronized(executionToken) {
            if (executionToken.closed) return
            executionToken.closed = true
            executionLocalLoaders.remove(executionToken)
        }
        executionLoaders?.forEach { it.close() }
        executionLoaders?.clear()
    }

    override fun close() {
        for (token in executionLocalLoaders.keys) {
            releaseExecutionLocalLoaders(token)
        }
        executionLocalLoaders.clear()
        cache.cleanUp()
    }

    private fun makeClassLoader(files: List<File>, parent: ClassLoader): URLClassLoader {
        logger.info("Creating new classloader for classpath: ${files.map { it.path }}")
        return URLClassLoader(files.map { it.toURI().toURL() }.toTypedArray(), parent)
    }

    private fun makeKey(files: List<File>): CacheKey {
        //probably should walk dirs content for actual last modified
        val entries = files.map { f -> ClasspathEntry(f, f.lastModified()) }
        return CacheKey(entries)
    }

    // Keyed by `File`, not `URL`: `URL.equals`/`hashCode` are protocol-handler operations and not plain value comparisons,
    // so `File` is more stable as a map key.
    private data class ClasspathEntry(val path: File, val modificationTimestamp: Long)

    private data class CacheKey(val entries: List<ClasspathEntry>)
}
