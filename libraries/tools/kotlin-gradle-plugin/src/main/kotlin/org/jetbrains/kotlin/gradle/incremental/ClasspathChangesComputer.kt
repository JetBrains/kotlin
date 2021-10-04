/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.gradle.incremental.ImpactAnalysis.computeImpactedSet
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.storage.FileToCanonicalPathConverter
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.io.File
import java.util.*

/** Computes [ClasspathChanges] between two [ClasspathSnapshot]s .*/
object ClasspathChangesComputer {

    fun compute(
        currentClasspathEntrySnapshotFiles: List<File>,
        previousClasspathEntrySnapshotFiles: List<File>,
        unchangedCurrentClasspathEntrySnapshotFiles: List<File>
    ): ClasspathChanges {
        // To improve performance, we will compute changes for the changed snapshot files only, ignoring unchanged ones. (Duplicate classes
        // will make this a bit tricky, but it will be dealt with below.)
        // First, align unchanged snapshot files in the current classpath with unchanged snapshot files in the previous classpath. Gradle
        // has this information, but doesn't expose it, so we have to reconstruct it here.
        val unchangedCurrentToPreviousAlignment: Map<File, File> =
            alignUnchangedSnapshotFiles(unchangedCurrentClasspathEntrySnapshotFiles, previousClasspathEntrySnapshotFiles)

        // Use sets to make presence checks faster
        val unchangedCurrentFiles: Set<File> = unchangedCurrentToPreviousAlignment.keys
        val unchangedPreviousFiles: Set<File> = unchangedCurrentToPreviousAlignment.values.toSet()

        // We will split the current files into 2 groups:
        //    1a) Unchanged current files
        //    1b) Added files
        // We will split the previous files into 2 groups:
        //    2a) Unchanged previous files
        //    2b) Removed files
        // If the classpath doesn't contain duplicate classes, comparing (1b) with (2b) would be enough.
        // However, if the classpath contains duplicate classes, comparing (1b) with (2b) would not be enough.
        // Therefore, to deal with duplicate classes while still being able to compare (1b) with (2b), we will find snapshot files in groups
        // (1a) and (2a) that have duplicate classes with groups (1b) or (2b) and add them to groups (1b) and (2b). Duplicate classes in
        // groups (1b) and (2b) will then be handled in a separate step (see ClasspathChangesComputer.getNonDuplicateClassSnapshots).
        val addedFiles: List<File> = currentClasspathEntrySnapshotFiles.filter { it !in unchangedCurrentFiles }
        val removedFiles: List<File> = previousClasspathEntrySnapshotFiles.filter { it !in unchangedPreviousFiles }

        val adjustedAddedFiles = addedFiles.toMutableSet()
        val adjustedRemovedFiles = removedFiles.toMutableSet()
        unchangedCurrentToPreviousAlignment.forEach { (unchangedCurrentFile, unchangedPreviousFile) ->
            if (unchangedCurrentFile.containsDuplicatesWith(addedFiles) || unchangedPreviousFile.containsDuplicatesWith(removedFiles)) {
                adjustedAddedFiles.add(unchangedCurrentFile)
                adjustedRemovedFiles.add(unchangedPreviousFile)
            }
        }

        // Keep the original order of added/removed files as it is important for the handling of duplicate classes.
        val finalAddedFiles: List<File> = currentClasspathEntrySnapshotFiles.filter { it in adjustedAddedFiles }
        val finalRemovedFiles: List<File> = previousClasspathEntrySnapshotFiles.filter { it in adjustedRemovedFiles }

        val changedCurrentSnapshot = ClasspathSnapshotSerializer.load(finalAddedFiles)
        val changedPreviousSnapshot = ClasspathSnapshotSerializer.load(finalRemovedFiles)

        return compute(changedCurrentSnapshot, changedPreviousSnapshot)
    }

    /**
     * Maps the unchanged snapshot files of the current build to the unchanged snapshot files of the previous build (selected from all the
     * snapshot files of the previous build).
     *
     * Note that the unchanged files of the current build were detected by Gradle, so a mapping must exist for each of them, we only have to
     * find it.
     *
     * IMPORTANT: The alignment algorithm must use the same input normalization that is used for snapshot files in the Gradle task.
     * Currently, snapshot files are annotated with `@Classpath` and are regular files (not jars), so (only) their contents and order
     * matter.
     */
    private fun alignUnchangedSnapshotFiles(unchangedCurrentSnapshotFiles: List<File>, previousSnapshotFiles: List<File>): Map<File, File> {
        val sizeToPreviousFiles: Map<Long, List<IndexedValue<File>>> = previousSnapshotFiles.withIndex().groupBy { it.value.length() }

        var startIndexToSearch = 0
        return unchangedCurrentSnapshotFiles.associateWith { unchangedCurrentFile ->
            val candidates = (sizeToPreviousFiles[unchangedCurrentFile.length()] ?: emptyList()).filter { it.index >= startIndexToSearch }
            val unchangedPreviousFileWithIndex: IndexedValue<File> = if (candidates.size == 1) {
                // A matching file must exist, so if there is only one candidate, it is the one.
                candidates.single()
            } else {
                // If there are multiple matching files, select the first one. (Even if it doesn't match Gradle's alignment, it is still a
                // correct alignment.)
                val unchangedContents = unchangedCurrentFile.readBytes()
                candidates.firstOrNull { candidate ->
                    unchangedContents.contentEquals(candidate.value.readBytes())
                } ?: error("Can't find previous snapshot file of unchanged current snapshot file '${unchangedCurrentFile.path}'")
            }
            startIndexToSearch = unchangedPreviousFileWithIndex.index + 1
            unchangedPreviousFileWithIndex.value
        }
    }

    /** Returns `true` if this snapshot file contains a duplicate class with another snapshot file in the given list. */
    @Suppress("unused", "UNUSED_PARAMETER")
    private fun File.containsDuplicatesWith(otherSnapshotFiles: List<File>): Boolean {
        // FIXME: Implement and optimize this method
        return false
    }

    fun compute(currentClasspathSnapshot: ClasspathSnapshot, previousClasspathSnapshot: ClasspathSnapshot): ClasspathChanges {
        val currentClassSnapshots = currentClasspathSnapshot.getNonDuplicateClassSnapshots()
        val previousClassSnapshots = previousClasspathSnapshot.getNonDuplicateClassSnapshots()

        return computeClassChanges(currentClassSnapshots, previousClassSnapshots)
    }

    /**
     * Returns all [ClassSnapshot]s in this [ClasspathSnapshot].
     *
     * If there are duplicate classes on the classpath, retain only the first one to match the compiler's behavior.
     */
    private fun ClasspathSnapshot.getNonDuplicateClassSnapshots(): List<ClassSnapshot> {
        val classSnapshots = LinkedHashMap<String, ClassSnapshot>(classpathEntrySnapshots.sumOf { it.classSnapshots.size })
        for (classpathEntrySnapshot in classpathEntrySnapshots) {
            for ((unixStyleRelativePath, classSnapshot) in classpathEntrySnapshot.classSnapshots) {
                classSnapshots.putIfAbsent(unixStyleRelativePath, classSnapshot)
            }
        }
        return classSnapshots.values.toList()
    }

    /**
     * Computes changes between two lists of [ClassSnapshot]s.
     *
     * Each list must not contain duplicate classes.
     */
    fun computeClassChanges(currentClassSnapshots: List<ClassSnapshot>, previousClassSnapshots: List<ClassSnapshot>): ClasspathChanges {
        if (currentClassSnapshots.any { it is ContentHashJavaClassSnapshot }
            || previousClassSnapshots.any { it is ContentHashJavaClassSnapshot }) {
            return ClasspathChanges.NotAvailable.UnableToCompute
        }

        // Ignore `EmptyJavaClassSnapshot`s as they don't impact the result
        val currentNonEmptyClassSnapshots = currentClassSnapshots.filter { it !is EmptyJavaClassSnapshot }
        val previousNonEmptyClassSnapshots = previousClassSnapshots.filter { it !is EmptyJavaClassSnapshot }

        val (currentAsmBasedSnapshots, currentProtoBasedSnapshots) =
            currentNonEmptyClassSnapshots.partition { it is RegularJavaClassSnapshot }
        val (previousAsmBasedSnapshots, previousProtoBasedSnapshots) =
            previousNonEmptyClassSnapshots.partition { it is RegularJavaClassSnapshot }

        val changeSet1 = computeChangesForProtoBasedSnapshots(currentProtoBasedSnapshots, previousProtoBasedSnapshots)

        @Suppress("UNCHECKED_CAST")
        val changeSet2 = JavaClassChangesComputer.compute(
            currentAsmBasedSnapshots as List<RegularJavaClassSnapshot>,
            previousAsmBasedSnapshots as List<RegularJavaClassSnapshot>
        )

        val allChanges = changeSet1 + changeSet2
        if (allChanges.isEmpty()) {
            return allChanges.toClasspathChanges()
        }

        val impactedSet = computeImpactedSet(allChanges, previousNonEmptyClassSnapshots)

        return impactedSet.toClasspathChanges()
    }

    private fun computeChangesForProtoBasedSnapshots(
        currentClassSnapshots: List<ClassSnapshot>,
        previousClassSnapshots: List<ClassSnapshot>
    ): ChangeSet {
        val workingDir =
            FileUtil.createTempDirectory(this::class.java.simpleName, "_WorkingDir_${UUID.randomUUID()}", /* deleteOnExit */ true)
        val incrementalJvmCache = IncrementalJvmCache(workingDir, /* targetOutputDir */ null, FileToCanonicalPathConverter)

        // Step 1:
        //   - Add previous class snapshots to incrementalJvmCache.
        //   - Internally, incrementalJvmCache maintains a set of dirty classes to detect removed classes. Add previous classes to this set
        //     to detect removed classes later (see step 2).
        //   - The ChangesCollector result will contain symbols in the previous classes (we actually don't need them, but it's part of the
        //     API's effects).
        val unusedChangesCollector = ChangesCollector()
        for (previousSnapshot in previousClassSnapshots) {
            when (previousSnapshot) {
                is KotlinClassSnapshot -> {
                    incrementalJvmCache.saveClassToCache(
                        kotlinClassInfo = previousSnapshot.classInfo,
                        sourceFiles = null,
                        changesCollector = unusedChangesCollector
                    )
                    incrementalJvmCache.markDirty(previousSnapshot.classInfo.className)
                }
                is ProtoBasedJavaClassSnapshot -> {
                    incrementalJvmCache.saveJavaClassProto(
                        source = null,
                        serializedJavaClass = previousSnapshot.serializedJavaClass,
                        collector = unusedChangesCollector
                    )
                    incrementalJvmCache.markDirty(JvmClassName.byClassId(previousSnapshot.serializedJavaClass.classId))
                }
                is RegularJavaClassSnapshot, is ContentHashJavaClassSnapshot, is EmptyJavaClassSnapshot -> {
                    error("Unexpected type (it should have been handled earlier): ${previousSnapshot.javaClass.name}")
                }
            }
        }

        // Step 2:
        //   - Add current class snapshots to incrementalJvmCache. This will overwrite any previous class snapshots that have the same
        //     `JvmClassName`. The remaining previous class snapshots will be removed in step 3.
        //   - Internally, incrementalJvmCache will remove current classes from the set of dirty classes. After this, the remaining dirty
        //     classes will be classes that are present on the previous classpath but not on the current classpath (i.e., removed classes).
        //   - The intermediate ChangesCollector result will contain symbols in added classes and changed (added/modified/removed) symbols
        //     in modified classes. We will collect symbols in removed classes in step 3.
        val changesCollector = ChangesCollector()
        for (currentSnapshot in currentClassSnapshots) {
            when (currentSnapshot) {
                is KotlinClassSnapshot -> {
                    incrementalJvmCache.saveClassToCache(
                        kotlinClassInfo = currentSnapshot.classInfo,
                        sourceFiles = null,
                        changesCollector = changesCollector
                    )
                }
                is ProtoBasedJavaClassSnapshot -> {
                    incrementalJvmCache.saveJavaClassProto(
                        source = null,
                        serializedJavaClass = currentSnapshot.serializedJavaClass,
                        collector = changesCollector
                    )
                }
                is RegularJavaClassSnapshot, is ContentHashJavaClassSnapshot, is EmptyJavaClassSnapshot -> {
                    error("Unexpected type (it should have been handled earlier): ${currentSnapshot.javaClass.name}")
                }
            }
        }

        // Step 3:
        //   - Detect removed classes: They are the remaining dirty classes.
        //   - Remove class snapshots of removed classes from incrementalJvmCache.
        //   - The final ChangesCollector result will contain symbols in added classes, changed (added/modified/removed) symbols in modified
        //     classes, and symbols in removed classes.
        incrementalJvmCache.clearCacheForRemovedClasses(changesCollector)

        // Normalize the changes and clean up
        val dirtyData = changesCollector.getDirtyData(listOf(incrementalJvmCache), EmptyICReporter)
        workingDir.deleteRecursively()

        return dirtyData.normalize(currentClassSnapshots, previousClassSnapshots)
    }

    private fun DirtyData.normalize(currentClassSnapshots: List<ClassSnapshot>, previousClassSnapshots: List<ClassSnapshot>): ChangeSet {
        val allClassIds = currentClassSnapshots.map { it.getClassId() }.toSet() + previousClassSnapshots.map { it.getClassId() }
        val fqNameToClassId = LinkedHashMap<FqName, ClassId>(allClassIds.size)
        allClassIds.forEach { classId ->
            val fqName = classId.asSingleFqName()
            check(!fqNameToClassId.contains(fqName)) {
                "Ambiguous FqName $fqName corresponds to two different `ClassId`s: ${fqNameToClassId[fqName]} and $classId"
            }
            fqNameToClassId[fqName] = classId
        }

        return ChangeSet.Collector().run {
            dirtyLookupSymbols.forEach {
                fqNameToClassId[FqName(it.scope)]?.let { classIdOfScope ->
                    // If scope is a class, lookup symbol is a class member and maybe inner class
                    fqNameToClassId[FqName("${it.scope}.${it.name}")]?.let { innerClass ->
                        addChangedClass(innerClass)
                    } ?: addChangedClassMember(classIdOfScope, it.name)
                    return@forEach
                }

                // scope is a package, so changed symbol is a top-level member and maybe a class
                val potentialClassFqName = if (it.scope.isEmpty()) FqName(it.name) else FqName("${it.scope}.${it.name}")
                fqNameToClassId[potentialClassFqName]?.let { classId ->
                    // Lookup symbol is a class
                    addChangedClass(classId)
                } ?: addChangedTopLevelMember(FqName(it.scope), it.name)
            }
            val changes = getChanges()

            // dirtyClassesFqNames should be derived from dirtyLookupSymbols. Double-check that this is the case.
            val changedFqNames: Set<FqName> =
                changes.changedClasses.map { it.asSingleFqName() }.toSet() +
                        changes.changedClassMembers.keys.map { it.asSingleFqName() } +
                        changes.changedTopLevelMembers.keys
            check(dirtyClassesFqNames.toSet() == changedFqNames) {
                "Two sets differ:\n" +
                        "dirtyClassesFqNames: $dirtyClassesFqNames\n" +
                        "changedFqNames: $changedFqNames"
            }
            changes
        }
    }
}

private fun ClassSnapshot.getClassId(): ClassId {
    return when (this) {
        is KotlinClassSnapshot -> classInfo.classId
        is RegularJavaClassSnapshot -> classId
        is ProtoBasedJavaClassSnapshot -> serializedJavaClass.classId
        is EmptyJavaClassSnapshot, is ContentHashJavaClassSnapshot -> {
            error("Unexpected type (it should have been handled earlier): ${javaClass.name}")
        }
    }
}

private object ImpactAnalysis {

    /**
     * Computes the set of classes/class members that are impacted by the given changes.
     *
     * For example, if a superclass has changed, any of its subclasses will be impacted even if it has not changed, and unchanged source
     * files in the previous compilation that depended on the subclasses will need to be recompiled.
     *
     * The returned set is also a [ChangeSet], which includes the given changes plus the impacted ones.
     */
    fun computeImpactedSet(changes: ChangeSet, previousClassSnapshots: List<ClassSnapshot>): ChangeSet {
        val classIdToSubclasses = getClassIdToSubclassesMap(previousClassSnapshots)

        return ChangeSet.Collector().run {
            addChangedClasses(findSubclassesInclusive(changes.changedClasses, classIdToSubclasses))
            for ((changedClass, changedClassMembers) in changes.changedClassMembers) {
                findSubclassesInclusive(setOf(changedClass), classIdToSubclasses).forEach {
                    addChangedClassMembers(it, changedClassMembers)
                }
            }
            for ((changedPackage, changedClassMembers) in changes.changedTopLevelMembers) {
                addChangedTopLevelMembers(changedPackage, changedClassMembers)
            }
            getChanges()
        }
    }

    private fun getClassIdToSubclassesMap(classSnapshots: List<ClassSnapshot>): Map<ClassId, Set<ClassId>> {
        val classIds = classSnapshots.map { it.getClassId() }
        val classNameToClassId = classIds.associateBy { JvmClassName.byClassId(it) }
        val classNameToClassIdResolver = { className: JvmClassName -> classNameToClassId[className] }

        val classIdToSubclasses = mutableMapOf<ClassId, MutableSet<ClassId>>()
        classSnapshots.forEach { classSnapshot ->
            val classId = classSnapshot.getClassId()
            classSnapshot.getSupertypes(classNameToClassIdResolver).forEach { supertype ->
                // No need to collect supertypes outside the considered class snapshots (e.g., "java/lang/Object")
                if (supertype in classIds) {
                    classIdToSubclasses.computeIfAbsent(supertype) { mutableSetOf() }.add(classId)
                }
            }
        }
        return classIdToSubclasses
    }

    private fun ClassSnapshot.getSupertypes(classIdResolver: (JvmClassName) -> ClassId?): List<ClassId> {
        return when (this) {
            is RegularJavaClassSnapshot -> supertypes.mapNotNull {
                // The following call returns null if supertype is outside the considered class snapshots (e.g., "java/lang/Object").
                // Use `mapNotNull` as we don't need to collect those supertypes (see getClassIdToSubclassesMap).
                classIdResolver.invoke(it)
            }
            is KotlinClassSnapshot -> supertypes.mapNotNull {
                // Same as above
                classIdResolver.invoke(it)
            }
            is ProtoBasedJavaClassSnapshot -> {
                val (proto, nameResolver) = serializedJavaClass.toProtoData()
                proto.supertypes(TypeTable(proto.typeTable)).map { nameResolver.getClassId(it.className) }
            }
            is EmptyJavaClassSnapshot, is ContentHashJavaClassSnapshot -> {
                error("Unexpected type (it should have been handled earlier): ${javaClass.name}")
            }
        }
    }

    /**
     * Finds direct and indirect subclasses of the given classes. The return set includes both the given classes and their direct and
     * indirect subclasses.
     */
    private fun findSubclassesInclusive(classIds: Set<ClassId>, classIdsToSubclasses: Map<ClassId, Set<ClassId>>): Set<ClassId> {
        val visitedClasses = mutableSetOf<ClassId>()
        val toVisitClasses = classIds.toMutableSet()
        while (toVisitClasses.isNotEmpty()) {
            val nextToVisit = mutableSetOf<ClassId>()
            toVisitClasses.forEach {
                nextToVisit.addAll(classIdsToSubclasses[it] ?: emptyList())
            }
            visitedClasses.addAll(toVisitClasses)
            toVisitClasses.clear()
            toVisitClasses.addAll(nextToVisit - visitedClasses)
        }
        return visitedClasses
    }
}
