/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import java.io.*

// TODO(gavra): switch away from Java serialization
class JavaClassCacheManager(val file: File, private val classpathFqNamesHistory: File) : Closeable {

    private val javaCacheFile = file.resolve("java-cache.bin")
    internal val javaCache = maybeGetJavaCacheFromFile()

    private val aptCacheFile = file.resolve("apt-cache.bin")
    private val aptCache = maybeGetAptCacheFromFile()

    private val lastBuildTimestamp = file.resolve("last-build-ts.bin")

    private var closed = false

    private fun getDirtyFqNamesFromClasspath(): ClasspathChanged {
        if (!lastBuildTimestamp.exists()) return ClasspathChanged.FullRebuild

        val lastTimestamp = lastBuildTimestamp.readText()

        val (before, after) = classpathFqNamesHistory.listFiles().partition { it.name < lastTimestamp }

        if (before.isEmpty()) {
            return ClasspathChanged.FullRebuild
        }

        val dirtyFqNames = mutableSetOf<String>()
        after.forEach { file ->
            ObjectInputStream(file.inputStream().buffered()).use {
                @Suppress("UNCHECKED_CAST")
                dirtyFqNames.addAll(it.readObject() as Collection<String>)
            }
        }

        return if (dirtyFqNames.isNotEmpty()) {
            // TODO(gavra): We need to handle constants from classpath that might change between runs being incremental. One solution
            // would be to fetch the changed symbols alongside changed fqNames, and to check if the symbol is a constant using ASM.
            ClasspathChanged.FullRebuild
        } else {
            ClasspathChanged.Incremental(dirtyFqNames)
        }
    }

    fun updateCache(processors: List<IncrementalProcessor>) {
        if (!aptCache.updateCache(processors)) {
            javaCache.invalidateAll()
        }
    }

    /**
     * From set of changed sources, get list of files to recompile using structural information and dependency information from
     * annotation processing.
     */
    fun invalidateAndGetDirtyFiles(changedSources: Collection<File>): SourcesToReprocess {
        if (!aptCache.isIncremental) {
            return SourcesToReprocess.FullRebuild
        }

        val dirtyFqNamesFromClasspath = getDirtyFqNamesFromClasspath()
        return when (dirtyFqNamesFromClasspath) {
            is ClasspathChanged.FullRebuild -> SourcesToReprocess.FullRebuild
            is ClasspathChanged.Incremental -> {
                val changes = Changes(changedSources, dirtyFqNamesFromClasspath.dirtyFqNames)
                val filesToReprocess = javaCache.invalidateEntriesForChangedFiles(changes)

                when (filesToReprocess) {
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

        with(lastBuildTimestamp) {
            writeText(System.currentTimeMillis().toString())
        }
        closed = true
    }
}

sealed class SourcesToReprocess {
    class Incremental(val toReprocess: List<File>, val dirtyTypes: Set<String>) : SourcesToReprocess()
    object FullRebuild : SourcesToReprocess()
}

sealed class ClasspathChanged {
    class Incremental(val dirtyFqNames: Set<String>) : ClasspathChanged()
    object FullRebuild : ClasspathChanged()
}