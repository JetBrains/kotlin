/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.FileToCanonicalPathConverter
import java.util.*
import kotlin.collections.LinkedHashMap

/** Computes [ClasspathChanges] between two [ClasspathSnapshot]s .*/
object ClasspathChangesComputer {

    fun compute(currentClasspathSnapshot: ClasspathSnapshot, previousClasspathSnapshot: ClasspathSnapshot): ClasspathChanges {
        val currentClassSnapshots = currentClasspathSnapshot.getClassSnapshots()
        val previousClassSnapshots = previousClasspathSnapshot.getClassSnapshots()

        if (currentClassSnapshots.any { it is ContentHashJavaClassSnapshot }
            || previousClassSnapshots.any { it is ContentHashJavaClassSnapshot }) {
            return ClasspathChanges.NotAvailable.UnableToCompute
        }

        val workingDir =
            FileUtil.createTempDirectory(this::class.java.simpleName, "_WorkingDir_${UUID.randomUUID()}", /* deleteOnExit */ true)
        val incrementalJvmCache = IncrementalJvmCache(workingDir, /* targetOutputDir */ null, FileToCanonicalPathConverter)

        // Store previous class snapshots in incrementalJvmCache, the returned ChangesCollector result is not used.
        val unusedChangesCollector = ChangesCollector()
        for (previousSnapshot in previousClassSnapshots) {
            when (previousSnapshot) {
                is KotlinClassSnapshot -> incrementalJvmCache.saveClassToCache(
                    kotlinClassInfo = previousSnapshot.classInfo,
                    sourceFiles = null,
                    changesCollector = unusedChangesCollector
                )
                is RegularJavaClassSnapshot -> incrementalJvmCache.saveJavaClassProto(
                    source = null,
                    serializedJavaClass = previousSnapshot.serializedJavaClass,
                    collector = unusedChangesCollector
                )
                is EmptyJavaClassSnapshot -> {
                    // Nothing to process
                }
                is ContentHashJavaClassSnapshot -> {
                    error("Unexpected type (it should have been handled earlier): ${previousSnapshot.javaClass.name}")
                }
            }
        }
        // Call the following method even though there are no removed classes, just in case the method updates the state of
        // incrementalJvmCache.
        incrementalJvmCache.clearCacheForRemovedClasses(unusedChangesCollector)

        // Compute changes between the current class snapshots and the previously stored snapshots, and save the result in changesCollector.
        val changesCollector = ChangesCollector()
        for (currentSnapshot in currentClassSnapshots) {
            when (currentSnapshot) {
                is KotlinClassSnapshot -> incrementalJvmCache.saveClassToCache(
                    kotlinClassInfo = currentSnapshot.classInfo,
                    sourceFiles = null,
                    changesCollector = changesCollector
                )
                is RegularJavaClassSnapshot -> incrementalJvmCache.saveJavaClassProto(
                    source = null,
                    serializedJavaClass = currentSnapshot.serializedJavaClass,
                    collector = changesCollector
                )
                is EmptyJavaClassSnapshot -> {
                    // Nothing to process
                }
                is ContentHashJavaClassSnapshot -> {
                    error("Unexpected type (it should have been handled earlier): ${currentSnapshot.javaClass.name}")
                }
            }
        }
        incrementalJvmCache.clearCacheForRemovedClasses(changesCollector)

        val dirtyData = changesCollector.getDirtyData(listOf(incrementalJvmCache), EmptyICReporter)
        workingDir.deleteRecursively()

        return ClasspathChanges.Available(
            lookupSymbols = LinkedHashSet(dirtyData.dirtyLookupSymbols),
            fqNames = LinkedHashSet(dirtyData.dirtyClassesFqNames)
        )
    }

    private fun ClasspathSnapshot.getClassSnapshots(): List<ClassSnapshot> {
        // If there are duplicate classes on the classpath, retain only the first one to match the compiler's behavior.
        // We still need to consider whether to remove duplicate classes based on the class file name or the `ClassId`, as two different
        // `ClassId`s can have the same class file name (e.g., nested class `B$C` of top-level class `A` in `first.jar` and nested class `C`
        // of top-level class `A$B` in `second.jar` both have the same class file name `A$B$C`).
        //   - If we use class file name, only `A$B$C` in `first.jar` will be retained. This matches the compiler's behavior because when
        //     resolving either of those classes, the compiler will only look for `A$B$C` in the first jar, even when the actual class is
        //     located in the second jar.
        //   - If we use `ClassId`, both `A$B$C` in `first.jar` and `A$B$C` in `second.jar` will be retained. That means that the snapshot
        //     of the class in `second.jar` will be considered when computing classpath changes, which is not expected as it doesn't match
        //     the compiler's behavior.
        // Therefore, we will remove duplicate classes based on the class file name, not the `ClassId`.
        val classSnapshots = LinkedHashMap<String, ClassSnapshot>(classpathEntrySnapshots.sumOf { it.classSnapshots.size })
        for (classpathEntrySnapshot in classpathEntrySnapshots) {
            for ((unixStyleRelativePath, classSnapshot) in classpathEntrySnapshot.classSnapshots) {
                classSnapshots.putIfAbsent(unixStyleRelativePath, classSnapshot)
            }
        }
        return classSnapshots.values.toList()
    }
}
