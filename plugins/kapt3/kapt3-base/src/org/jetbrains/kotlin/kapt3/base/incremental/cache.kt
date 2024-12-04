/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import java.io.*
import java.util.ArrayDeque

// TODO(gavra): switch away from Java serialization
class JavaClassCacheManager(val file: File) : Closeable {

    private val javaCacheFile = file.resolve("java-cache.bin")
    internal val javaCache = maybeGetJavaCacheFromFile()

    private val aptCacheFile = file.resolve("apt-cache.bin")
    private val aptCache = maybeGetAptCacheFromFile()

    private var closed = false

    fun updateCache(processors: List<IncrementalProcessor>, failedToAnalyzeSources: Boolean) {
        if (!aptCache.updateCache(processors, failedToAnalyzeSources)) {
            javaCache.invalidateAll()
            return
        }
        // Compilation is fully incremental, record types defined in generated .class files
        processors.forEach { processor ->
            processor.getGeneratedClassFilesToTypes().forEach { (classFile, type) ->
                val classFileStructure = ClassFileStructure(classFile.toURI(), type)
                javaCache.addSourceStructure(classFileStructure)
            }
        }
    }

    /**
     * From set of changed sources, get list of files to recompile using structural information and dependency information from
     * annotation processing.
     */
    fun invalidateAndGetDirtyFiles(
        changedSources: Collection<File>,
        dirtyClasspathJvmNames: Collection<String>,
        compiledSources: List<File>
    ): SourcesToReprocess {
        if (!aptCache.isIncremental) {
            return SourcesToReprocess.FullRebuild
        }

        /**
         * Incremental annotation processing tries to limit the number of .java files that are passes to annotation processors, and it
         * uses already existing .class files to resolve types. This relies on outputs of kotlinc and javac being present.
         *
         * The check below verifies that the number of types defined in the processed .java files matches the number of .class files
         * in the compiled sources. This check does not guarantee that .class file will indeed exist for every defined type, but
         * even if such check is introduced, there is no guarantee that the actual content of the source file matches the .class file. E.g:
         * - clean build i.e non-incremental KAPT
         * - modify B.java with error in the method body: KAPT runs successfully, but java compilation will fail (assume old B.class is kept)
         * - add C.java that references B as method parameter type: KAPT wil only process C.java, and it will use old version of B
         * To properly fix this, KAPT needs to keep track of kotlinc and javac compilation outcome, and there is no clean API to do that.
         *
         * However, comparing total numbers is probably good enough, and it should eliminate most of the issues. See KT-41456 for details.
         */
        val totalDeclaredTypes = javaCache.getSourceFileDefinedTypesCount()
        val compileOutputHasEnoughClassFiles = checkMinNumberOfClassFiles(compiledSources, totalDeclaredTypes)
        if (!compileOutputHasEnoughClassFiles) {
            return SourcesToReprocess.FullRebuild
        }

        val dirtyClasspathFqNames = HashSet<String>(dirtyClasspathJvmNames.size)
        dirtyClasspathJvmNames.forEach {
            dirtyClasspathFqNames.add(it.replace("$", ".").replace("/", "."))
        }

        val changes = Changes(changedSources, dirtyClasspathFqNames.toSet())
        val aggregatingGeneratedTypes = aptCache.getAggregatingGeneratedTypes(javaCache::getTypesForFiles)
        val impactedTypes = getAllImpactedTypes(changes, aggregatingGeneratedTypes)
        val isolatingGeneratedTypes = aptCache.getIsolatingGeneratedTypes(javaCache::getTypesForFiles)

        val sourcesToReprocess = changedSources.toMutableSet()
        val classNamesToReprocess = mutableListOf<String>()

        if (changedSources.isNotEmpty() || impactedTypes.isNotEmpty()) {
            for (aggregatingOrigin in aptCache.getAggregatingOrigins()) {
                if (aggregatingOrigin in impactedTypes) continue

                val originSource = javaCache.getSourceForType(aggregatingOrigin)
                if (originSource.extension == "java") {
                    sourcesToReprocess.add(originSource)
                } else if (originSource.extension == "class") {
                    // This is a generated .class file that we need to reprocess.
                    classNamesToReprocess.add(aggregatingOrigin)
                }
            }
        }

        for (impactedType in impactedTypes) {
            if (impactedType !in isolatingGeneratedTypes && impactedType !in aggregatingGeneratedTypes) {
                sourcesToReprocess.add(javaCache.getSourceForType(impactedType))
            } else if (impactedType in isolatingGeneratedTypes) {
                // this is a generated type by isolating AP
                val isolatingOrigin = aptCache.getOriginForGeneratedIsolatingType(impactedType, javaCache::getSourceForType)
                if (isolatingOrigin in impactedTypes || isolatingOrigin in dirtyClasspathFqNames) {
                    continue
                }
                val originSource = javaCache.getSourceForType(isolatingOrigin)
                if (originSource.extension == "java") {
                    sourcesToReprocess.add(originSource)
                } else if (originSource.extension == "class") {
                    classNamesToReprocess.add(isolatingOrigin)
                }
            }
        }

        if (sourcesToReprocess.isNotEmpty() || classNamesToReprocess.isNotEmpty()) {
            // Invalidate state only if there are some files that will be reprocessed
            javaCache.invalidateDataForTypes(impactedTypes)
            aptCache.invalidateAggregating()
            // for isolating, invalidate both own types and classpath types
            aptCache.invalidateIsolatingForOriginTypes(impactedTypes + dirtyClasspathFqNames)
        }

        return SourcesToReprocess.Incremental(sourcesToReprocess.toList(), impactedTypes, classNamesToReprocess)
    }

    private fun getAllImpactedTypes(changes: Changes, aggregatingGeneratedTypes: Set<String>): MutableSet<String> {
        val impactedTypes = javaCache.getAllImpactedTypes(changes)
        // check isolating with origins from the classpath
        impactedTypes.addAll(aptCache.getIsolatingGeneratedTypesForOrigins(changes.dirtyFqNamesFromClasspath, javaCache::getTypesForFiles))
        if (changes.sourceChanges.isNotEmpty() || impactedTypes.isNotEmpty()) {
            // Any source change or any source impacted by type change invalidates aggregating APs generated types
            impactedTypes.addAll(aggregatingGeneratedTypes)
        }
        // now check isolating with origins in any of the impacted types
        aptCache.getIsolatingGeneratedTypesForOrigins(impactedTypes, javaCache::getTypesForFiles).let {
            impactedTypes.addAll(it)
        }
        return impactedTypes
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
    class Incremental(
        val toReprocess: List<File>,
        val dirtyTypes: Set<String>,
        val unchangedAggregatedTypes: List<String>
    ) : SourcesToReprocess()

    object FullRebuild : SourcesToReprocess()
}

/** Returns if specified root dirs have at least [required] number of class files. */
private fun checkMinNumberOfClassFiles(roots: List<File>, required: Int): Boolean {
    var currentlyMissing = required
    roots.filter { it.isDirectory }.forEach {
        val inThisRoot = countClassFilesUpToLimit(it, currentlyMissing)
        currentlyMissing -= inThisRoot
    }
    return currentlyMissing <= 0
}

private fun countClassFilesUpToLimit(root: File, limit: Int): Int {
    var cnt = 0;
    val toVisit = ArrayDeque<File>()
    toVisit.add(root)
    while (cnt < limit && toVisit.isNotEmpty()) {
        val curr = toVisit.removeFirst()
        if (curr.isFile && curr.toString().endsWith(".class")) {
            cnt++
        } else if (curr.isDirectory) {
            curr.listFiles()?.forEach {
                toVisit.addLast(it)
            }
        }
    }

    return cnt
}