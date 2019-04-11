/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import java.io.*
import java.util.*

open class ClasspathSnapshot protected constructor(
    private val cacheDir: File,
    private val classpath: Iterable<File>,
    val dataForFiles: (Set<File>) -> Map<File, ClasspathEntryData>
) {
    val classpathData: (Set<File>) -> Map<File, ClasspathEntryData> = { files ->
        val missingFiles = files.filter { !computedClasspathData.keys.contains(it) }.toSet()

        if (!missingFiles.isEmpty()) {
            val computedData = dataForFiles(missingFiles)
            computedClasspathData.putAll(computedData)
        }
        computedClasspathData
    }
    private val computedClasspathData: MutableMap<File, ClasspathEntryData> = mutableMapOf()

    object ClasspathSnapshotFactory {
        fun loadFrom(cacheDir: File): ClasspathSnapshot {
            val classpathEntries = cacheDir.resolve("classpath-entries.bin")
            val classpathStructureData = cacheDir.resolve("classpath-structure.bin")
            if (!classpathEntries.exists() || !classpathStructureData.exists()) {
                return UnknownSnapshot
            }

            val classpathFiles = ObjectInputStream(BufferedInputStream(classpathEntries.inputStream())).use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as Iterable<File>
            }

            val classpathData = { _: Set<File> ->
                loadPreviousData(classpathStructureData)
            }
            return ClasspathSnapshot(cacheDir, classpathFiles, classpathData)
        }

        fun createCurrent(cacheDir: File, classpath: Iterable<File>, lazyClasspathData: Set<File>): ClasspathSnapshot {
            val lazyData = lazyClasspathData.map { LazyClasspathEntryData.LazyClasspathEntrySerializer.loadFromFile(it) }
            val data = { files: Set<File> ->
                lazyData.filter { files.contains(it.classpathEntry) }.associate { it.classpathEntry to it.getClasspathEntryData() }
            }

            return ClasspathSnapshot(cacheDir, classpath, data)
        }

        private fun loadPreviousData(file: File): Map<File, ClasspathEntryData> {
            ObjectInputStream(BufferedInputStream(file.inputStream())).use {
                @Suppress("UNCHECKED_CAST")
                return it.readObject() as Map<File, ClasspathEntryData>
            }
        }
    }

    private fun isCompatible(snapshot: ClasspathSnapshot) =
        this != UnknownSnapshot && classpath == snapshot.classpath

    /** Compare this snapshot with the specified one only for the specified files. */
    fun diff(previousSnapshot: ClasspathSnapshot, changedFiles: Set<File>): KaptClasspathChanges {
        if (!isCompatible(previousSnapshot)) {
            return KaptClasspathChanges.Unknown
        }

        val currentData = classpathData(changedFiles)
        val previousData = previousSnapshot.classpathData(changedFiles)

        val changedClasses = mutableSetOf<String>()

        for (changed in changedFiles) {
            val previous = previousData.getValue(changed)
            val current = currentData.getValue(changed)

            for (key in previous.classAbiHash.keys + current.classAbiHash.keys) {
                val previousHash = previous.classAbiHash[key]
                if (previousHash == null) {
                    changedClasses.add(key)
                    continue
                }
                val currentHash = current.classAbiHash[key]
                if (currentHash == null) {
                    changedClasses.add(key)
                    continue
                }
                if (!previousHash.contentEquals(currentHash)) {
                    changedClasses.add(key)
                }
            }
        }

        // We do not compute structural data for unchanged files of the current snapshot for performance reasons.
        // That is why we reuse the previous snapshot as that one contains all unchanged entries.
        previousData.filterTo(computedClasspathData) { (key, _) -> key !in computedClasspathData }

        val allImpactedClasses = findAllImpacted(changedClasses)

        return KaptClasspathChanges.Known(allImpactedClasses)
    }

    fun writeToCache() {
        val classpathEntries = cacheDir.resolve("classpath-entries.bin")
        ObjectOutputStream(BufferedOutputStream(classpathEntries.outputStream())).use {
            it.writeObject(classpath)
        }

        val classpathStructureData = cacheDir.resolve("classpath-structure.bin")
        storeCurrentStructure(classpathStructureData, classpathData(classpath.toSet()))
    }

    private fun storeCurrentStructure(file: File, structure: Map<File, ClasspathEntryData>) {
        ObjectOutputStream(BufferedOutputStream(file.outputStream())).use {
            it.writeObject(structure)
        }
    }

    private fun findAllImpacted(changedClasses: Set<String>): Set<String> {
        // TODO (gavra): Avoid building all reverse lookups. Most changes are local to the classpath entry, use that.
        val transitiveDeps = HashMap<String, MutableList<String>>()
        val nonTransitiveDeps = HashMap<String, MutableList<String>>()

        for (entry in computedClasspathData.values) {
            for ((className, classDependency) in entry.classDependencies) {
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

object UnknownSnapshot : ClasspathSnapshot(File(""), emptyList(), { emptyMap() })

sealed class KaptIncrementalChanges {
    object Unknown : KaptIncrementalChanges()
    class Known(val changedSources: Set<File>, val changedClasspathJvmNames: Set<String>) : KaptIncrementalChanges()
}

sealed class KaptClasspathChanges {
    object Unknown : KaptClasspathChanges()
    class Known(val names: Set<String>) : KaptClasspathChanges()
}