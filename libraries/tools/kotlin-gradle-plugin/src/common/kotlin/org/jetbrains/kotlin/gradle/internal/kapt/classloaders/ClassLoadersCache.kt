/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.classloaders

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * LRU cache for [ClassLoader]s by class path.
 */
class ClassLoadersCache(
    size: Int,
    private val parentClassLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
    ttl: Duration = Duration.ofHours(1)
) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(ClassLoadersCache::class.java)

    private val guavaCache: Cache<CacheKey, URLClassLoader> =
        CacheBuilder
            .newBuilder()
            .maximumSize(size.toLong())
            .expireAfterAccess(ttl)
            .removalListener<CacheKey, URLClassLoader> { (key, cl) ->
                check(key != null && cl != null)
                logger.info("Removing classloader from cache: ${key.entries.map { it.path }}")
                cl.close()
            }
            .build<CacheKey, URLClassLoader>()

    private val cache: ConcurrentMap<CacheKey, URLClassLoader> = guavaCache.asMap()

    /**
     * Class loaders created for classpath entries that must not be retained (see [getForSplitPaths]),
     * keyed by the thread that asked for them so that concurrent kapt executions in one daemon do not
     * close each other's loaders.
     */
    private val transientLoaders = ConcurrentHashMap<Thread, MutableList<URLClassLoader>>()

    fun getForClassPath(files: List<File>): ClassLoader = getForClassPath(files, parentClassLoader)

    private fun getForClassPath(files: List<File>, parent: ClassLoader): ClassLoader {
        val key = makeKey(files)
        val classLoader = cache.getOrPut(key) {
            makeClassLoader(key, parent)
        }
        // Guava delivers removal notifications during subsequent cache operations. This cache is
        // touched about once per kapt task, so without an explicit cleanUp an evicted loader can go
        // unclosed - and keep its jars open - indefinitely.
        guavaCache.cleanUp()
        return classLoader
    }

    /**
     * Gets a [ClassLoader] for [bottom] + [top] files.
     *
     * Only the [top] loader is cached. [bottom] holds project-local artifacts, so a loader over it is
     * created fresh for this execution and must be released with [releaseTransientLoader] once
     * annotation processing has finished - otherwise the cache keeps the project's own jars open for
     * the lifetime of the daemon, which on Windows prevents the project directory from being deleted.
     *
     * Useful when you have internal and external artifacts and internal ones can be references from other internal artefacts only.
     * So you can safely cache [ClassLoader] from external artifacts and use it for internal ones.
     */
    fun getForSplitPaths(bottom: List<File>, top: List<File>): ClassLoader {
        // Only external artifacts are cached. Note `top` is empty whenever every annotation processor
        // is a project dependency - caching `bottom + top` in that case was the file-descriptor leak.
        val parent = if (top.isEmpty()) parentClassLoader else getForClassPath(top)
        if (bottom.isEmpty()) return parent

        val local = makeClassLoader(makeKey(bottom), parent)
        // Not closed here: loaders handed out earlier in the same execution may still be in use.
        transientLoaders.computeIfAbsent(Thread.currentThread()) { mutableListOf() }.add(local)
        return local
    }

    /**
     * Closes the loaders [getForSplitPaths] created for project-local artifacts on this thread, if any.
     * Safe to call when there are none.
     */
    fun releaseTransientLoader() {
        transientLoaders.remove(Thread.currentThread())?.forEach { it.close() }
    }

    override fun close() {
        transientLoaders.values.forEach { loaders -> loaders.forEach { it.close() } }
        transientLoaders.clear()
        cache.clear()
        guavaCache.cleanUp()
    }

    private fun makeClassLoader(key: CacheKey, parent: ClassLoader): URLClassLoader {
        val cp = key.entries.map { it.path }
        logger.info("Creating new classloader for classpath: $cp")
        return URLClassLoader(cp.map { it.toURI().toURL() }.toTypedArray(), parent)
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
