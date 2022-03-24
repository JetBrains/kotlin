/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import java.io.*
import java.util.*
import kotlin.collections.HashMap

private const val CLASSPATH_ENTRIES_FILE = "classpath-entries.bin"
private const val ANNOTATION_PROCESSOR_CLASSPATH_ENTRIES_FILE = "ap-classpath-entries.bin"
private const val CLASSPATH_STRUCTURE_FILE = "classpath-structure.bin"

open class ClasspathSnapshot protected constructor(
    private val cacheDir: File,
    private val classpath: List<File>,
    private val annotationProcessorClasspath: List<File>,
    private val dataForFiles: MutableMap<File, ClasspathEntryData?>
) {
    object ClasspathSnapshotFactory {
        fun loadFrom(cacheDir: File): ClasspathSnapshot {
            val classpathEntries = cacheDir.resolve(CLASSPATH_ENTRIES_FILE)
            val classpathStructureData = cacheDir.resolve(CLASSPATH_STRUCTURE_FILE)
            val annotationProcessorClasspathEntries= cacheDir.resolve(ANNOTATION_PROCESSOR_CLASSPATH_ENTRIES_FILE)
            if (!classpathEntries.exists() || !classpathStructureData.exists() || !annotationProcessorClasspathEntries.exists()) {
                return UnknownSnapshot
            }

            val classpathFiles = ObjectInputStream(BufferedInputStream(classpathEntries.inputStream())).use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as List<File>
            }

            val annotationProcessorClasspathFiles =
                ObjectInputStream(BufferedInputStream(annotationProcessorClasspathEntries.inputStream())).use {
                    @Suppress("UNCHECKED_CAST")
                    it.readObject() as List<File>
                }

            val dataForFiles =
                ObjectInputStream(BufferedInputStream(classpathStructureData.inputStream())).use {
                    @Suppress("UNCHECKED_CAST")
                    it.readObject() as MutableMap<File, ClasspathEntryData?>
                }
            return ClasspathSnapshot(cacheDir, classpathFiles, annotationProcessorClasspathFiles, dataForFiles)
        }

        fun createCurrent(cacheDir: File, classpath: List<File>, annotationProcessorClasspath: List<File>, allStructureData: Set<File>): ClasspathSnapshot {
            val data = allStructureData.associateTo(HashMap<File, ClasspathEntryData?>(allStructureData.size)) { it to null }

            return ClasspathSnapshot(cacheDir, classpath, annotationProcessorClasspath, data)
        }

        fun getEmptySnapshot() = UnknownSnapshot
    }

    fun getAllDataFiles() = dataForFiles.keys

    private fun isCompatible(snapshot: ClasspathSnapshot) =
        this != UnknownSnapshot
                && snapshot != UnknownSnapshot
                && classpath == snapshot.classpath
                && annotationProcessorClasspath == snapshot.annotationProcessorClasspath

    /** Compare this snapshot with the specified one only for the specified files. */
    fun diff(previousSnapshot: ClasspathSnapshot, changedFiles: Set<File>): KaptClasspathChanges {
        if (!isCompatible(previousSnapshot)) {
            return KaptClasspathChanges.Unknown
        }
        if (annotationProcessorClasspath.any { it in changedFiles }) {
            // in case annotation processor classpath changes, we have to run non-incrementally
            return KaptClasspathChanges.Unknown
        }

        val unchangedBetweenCompilations = dataForFiles.keys.intersect(previousSnapshot.dataForFiles.keys).filter { it !in changedFiles }
        val currentToLoad = dataForFiles.keys.filter { it !in unchangedBetweenCompilations }.also { loadEntriesFor(it) }
        val previousToLoad = previousSnapshot.dataForFiles.keys.filter { it !in unchangedBetweenCompilations }

        check(currentToLoad.size == previousToLoad.size) {
            """
            Number of loaded files in snapshots differs. Reported changed files: $changedFiles
            Current snapshot data files: ${dataForFiles.keys}
            Previous snapshot data files: ${previousSnapshot.dataForFiles.keys}
        """.trimIndent()
        }

        val currentHashesToAnalyze = getHashesToAnalyze(currentToLoad)
        val previousHashesToAnalyze = previousSnapshot.getHashesToAnalyze(previousToLoad)

        val changedClasses = mutableSetOf<String>()
        for (key in previousHashesToAnalyze.keys + currentHashesToAnalyze.keys) {
            val previousHash = previousHashesToAnalyze[key]
            if (previousHash == null) {
                changedClasses.add(key)
                continue
            }
            val currentHash = currentHashesToAnalyze[key]
            if (currentHash == null) {
                changedClasses.add(key)
                continue
            }
            if (!previousHash.contentEquals(currentHash)) {
                changedClasses.add(key)
            }
        }

        // We do not compute structural data for unchanged files of the current snapshot for performance reasons.
        // That is why we reuse the previous snapshot as that one contains all unchanged entries.
        for (unchanged in unchangedBetweenCompilations) {
            dataForFiles[unchanged] = previousSnapshot.dataForFiles[unchanged]!!
        }

        val allImpactedClasses = findAllImpacted(changedClasses)

        return KaptClasspathChanges.Known(allImpactedClasses)
    }

    private fun getHashesToAnalyze(filesToLoad: List<File>): HashMap<String, ByteArray> {
        val hashAbiSize = filesToLoad.sumBy { dataForFiles[it]!!.classAbiHash.size }
        return HashMap<String, ByteArray>(hashAbiSize).also { hashes ->
            filesToLoad.forEach {
                hashes.putAll(dataForFiles[it]!!.classAbiHash)
            }
        }
    }

    private fun loadEntriesFor(file: Iterable<File>) {
        for (f in file) {
            if (dataForFiles[f] == null) {
                dataForFiles[f] = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(f)
            }
        }
    }

    private fun loadAll() {
        loadEntriesFor(dataForFiles.keys)
    }

    fun writeToCache() {
        loadAll()

        val classpathEntries = cacheDir.resolve(CLASSPATH_ENTRIES_FILE)
        ObjectOutputStream(BufferedOutputStream(classpathEntries.outputStream())).use {
            it.writeObject(classpath)
        }

        val annotationProcessorClasspathEntries = cacheDir.resolve(ANNOTATION_PROCESSOR_CLASSPATH_ENTRIES_FILE)
        ObjectOutputStream(BufferedOutputStream(annotationProcessorClasspathEntries.outputStream())).use {
            it.writeObject(annotationProcessorClasspath)
        }

        val classpathStructureData = cacheDir.resolve(CLASSPATH_STRUCTURE_FILE)
        ObjectOutputStream(BufferedOutputStream(classpathStructureData.outputStream())).use {
            it.writeObject(dataForFiles)
        }
    }

    private fun findAllImpacted(changedClasses: Set<String>): Set<String> {
        // TODO (gavra): Avoid building all reverse lookups. Most changes are local to the classpath entry, use that.
        val transitiveDeps = HashMap<String, MutableList<String>>()
        val nonTransitiveDeps = HashMap<String, MutableList<String>>()

        for (entry in dataForFiles.values) {
            for ((className, classDependency) in entry!!.classDependencies) {
                for (abiType in classDependency.abiTypes) {
                    (transitiveDeps[abiType] ?: LinkedList()).let {
                        it.add(className)
                        transitiveDeps[abiType] = it
                    }
                }
                for (privateType in classDependency.privateTypes) {
                    (nonTransitiveDeps[privateType] ?: LinkedList()).let {
                        it.add(className)
                        nonTransitiveDeps[privateType] = it
                    }

                }
            }
        }

        val allImpacted = mutableSetOf<String>()
        var current = changedClasses
        while (current.isNotEmpty()) {
            val newRound = mutableSetOf<String>()
            for (klass in current) {
                if (allImpacted.add(klass)) {
                    transitiveDeps[klass]?.let {
                        newRound.addAll(it)
                    }

                    nonTransitiveDeps[klass]?.let {
                        allImpacted.addAll(it)
                    }
                }
            }
            current = newRound
        }

        return allImpacted
    }
}

object UnknownSnapshot : ClasspathSnapshot(File(""), emptyList(), emptyList(), mutableMapOf())

sealed class KaptIncrementalChanges {
    object Unknown : KaptIncrementalChanges()
    class Known(val changedSources: Set<File>, val changedClasspathJvmNames: Set<String>) : KaptIncrementalChanges()
}

sealed class KaptClasspathChanges {
    object Unknown : KaptClasspathChanges()
    class Known(val names: Set<String>) : KaptClasspathChanges()
}