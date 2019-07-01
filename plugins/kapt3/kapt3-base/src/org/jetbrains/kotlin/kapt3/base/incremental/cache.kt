/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import java.io.*

// TODO(gavra): switch away from Java serialization
class JavaClassCacheManager(val file: File) : Closeable {

    private val javaCacheFile = file.resolve("java-cache.bin")
    internal val javaCache = maybeGetJavaCacheFromFile()

    private val aptCacheFile = file.resolve("apt-cache.bin")
    private val aptCache = maybeGetAptCacheFromFile()

    private var closed = false

    fun updateCache(processors: List<IncrementalProcessor>) {
        if (!aptCache.updateCache(processors)) {
            javaCache.invalidateAll()
        }
    }

    /**
     * From set of changed sources, get list of files to recompile using structural information and dependency information from
     * annotation processing.
     */
    fun invalidateAndGetDirtyFiles(changedSources: Collection<File>, dirtyClasspathJvmNames: Collection<String>): SourcesToReprocess {
        if (!aptCache.isIncremental) {
            return SourcesToReprocess.FullRebuild
        }

        val dirtyClasspathFqNames = HashSet<String>(dirtyClasspathJvmNames.size)
        dirtyClasspathJvmNames.forEach {
            dirtyClasspathFqNames.add(it.replace("$", ".").replace("/", "."))
        }

        val changes = Changes(changedSources, dirtyClasspathFqNames.toSet())
        val filesToReprocess = javaCache.invalidateEntriesForChangedFiles(changes)

        return when (filesToReprocess) {
            is SourcesToReprocess.FullRebuild -> SourcesToReprocess.FullRebuild
            is SourcesToReprocess.Incremental -> {
                val toReprocess = filesToReprocess.toReprocess.toMutableSet()

                val isolatingGenerated = aptCache.invalidateIsolatingGenerated(toReprocess)
                val generatedDirtyTypes = javaCache.invalidateGeneratedTypes(isolatingGenerated).toMutableSet()

                if (!toReprocess.isEmpty()) {
                    // only if there are some files to reprocess we should invalidate the aggregating ones
                    val aggregatingGenerated = aptCache.invalidateAggregating()
                    generatedDirtyTypes.addAll(javaCache.invalidateGeneratedTypes(aggregatingGenerated))

                    toReprocess.addAll(
                        javaCache.invalidateEntriesAnnotatedWith(aptCache.getAggregatingClaimedAnnotations())
                    )
                }

                SourcesToReprocess.Incremental(toReprocess.toList(), generatedDirtyTypes)
            }
        }
    }

    private fun maybeGetAptCacheFromFile(): IncrementalAptCache {

        return if (aptCacheFile.exists()) {
            try {
                ObjectInputStream(BufferedInputStream(aptCacheFile.inputStream())).use {
                    it.readObject() as IncrementalAptCache
                }
            } catch (e: Throwable) {
                // cache corrupt
                IncrementalAptCache()
            }
        } else {
            IncrementalAptCache()
        }
    }

    private fun maybeGetJavaCacheFromFile(): JavaClassCache {
        return if (javaCacheFile.exists()) {
            try {
                ObjectInputStream(BufferedInputStream(javaCacheFile.inputStream())).use {
                    it.readObject() as JavaClassCache
                }
            } catch (e: Throwable) {
                JavaClassCache()
            }
        } else {
            JavaClassCache()
        }
    }

    override fun close() {
        if (closed) return

        with(javaCacheFile) {
            delete()
            parentFile.mkdirs()
            ObjectOutputStream(BufferedOutputStream(outputStream())).use {
                it.writeObject(javaCache)
            }
        }

        with(aptCacheFile) {
            delete()
            parentFile.mkdirs()
            ObjectOutputStream(BufferedOutputStream(outputStream())).use {
                it.writeObject(aptCache)
            }
        }

        closed = true
    }
}

sealed class SourcesToReprocess {
    class Incremental(val toReprocess: List<File>, val dirtyTypes: Set<String>) : SourcesToReprocess()
    object FullRebuild : SourcesToReprocess()
}