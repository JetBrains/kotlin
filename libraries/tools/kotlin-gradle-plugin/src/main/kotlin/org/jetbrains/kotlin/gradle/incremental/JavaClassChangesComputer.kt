/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.gradle.incremental.ImpactAnalysis.computeImpactedSet
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME

/** Computes [ChangeSet] between two lists of [JavaClassSnapshot]s .*/
object JavaClassChangesComputer {

    /**
     * Computes [ChangeSet] between two lists of [JavaClassSnapshot]s.
     *
     * Each list must not contain duplicate classes (having the same [JvmClassName]/[ClassId]).
     */
    fun compute(
        currentJavaClassSnapshots: List<RegularJavaClassSnapshot>,
        previousJavaClassSnapshots: List<RegularJavaClassSnapshot>
    ): ChangeSet {
        val currentClasses: Map<ClassId, RegularJavaClassSnapshot> = currentJavaClassSnapshots.associateBy { it.classId }
        val previousClasses: Map<ClassId, RegularJavaClassSnapshot> = previousJavaClassSnapshots.associateBy { it.classId }

        // No need to collect added classes as they don't impact recompilation
        val removedClasses = previousClasses.keys - currentClasses.keys
        val unchangedOrModifiedClasses = previousClasses.keys - removedClasses

        val changes = ChangeSet.Collector().run {
            addChangedClasses(removedClasses)
            unchangedOrModifiedClasses.forEach {
                collectClassChanges(currentClasses[it]!!, previousClasses[it]!!, this)
            }
            getChanges()
        }

        return computeImpactedSet(changes, previousJavaClassSnapshots)
    }

    /**
     * Collects changes between two [JavaClassSnapshot]s.
     *
     * The two classes must have the same [ClassId].
     */
    private fun collectClassChanges(
        currentClassSnapshot: RegularJavaClassSnapshot,
        previousClassSnapshot: RegularJavaClassSnapshot,
        changes: ChangeSet.Collector
    ) {
        val classId = currentClassSnapshot.classId.also { check(it == previousClassSnapshot.classId) }
        if (currentClassSnapshot.classAbiExcludingMembers.abiHash != previousClassSnapshot.classAbiExcludingMembers.abiHash) {
            changes.addChangedClass(classId)
        } else {
            collectClassMemberChanges(classId, currentClassSnapshot.fieldsAbi, previousClassSnapshot.fieldsAbi, changes)
            collectClassMemberChanges(classId, currentClassSnapshot.methodsAbi, previousClassSnapshot.methodsAbi, changes)
        }
    }

    /** Collects changes between two lists of fields/methods within a class. */
    private fun collectClassMemberChanges(
        classId: ClassId,
        currentMemberSnapshots: List<AbiSnapshot>,
        previousMemberSnapshots: List<AbiSnapshot>,
        changes: ChangeSet.Collector
    ) {
        val currentMemberHashes: Set<Long> = currentMemberSnapshots.map { it.abiHash }.toSet()
        val previousMemberHashes: Map<Long, AbiSnapshot> = previousMemberSnapshots.associateBy { it.abiHash }

        val addedMembers = currentMemberHashes - previousMemberHashes.keys
        val removedMembers = previousMemberHashes.keys - currentMemberHashes

        // Note:
        //   - No need to collect added members as they don't impact recompilation.
        //   - Modified members have a current version and a previous version. The current version will appear in addedMembers (which will
        //     not be collected), and the previous version will appear in removedMembers (which will be collected).
        //   - Multiple members may have the same name (but never the same signature (name + desc) or ABI hash). It's okay to report the
        //     same name multiple times.
        changes.addChangedClassMembers(classId, removedMembers.map { previousMemberHashes[it]!!.name })

        // TODO: Check whether the condition to add SAM_LOOKUP_NAME below is too broad, and correct it if necessary.
        // Currently, it matches the logic in ChangesCollector.getDirtyData in buildUtil.kt.
        if (addedMembers.isNotEmpty() || removedMembers.isNotEmpty()) {
            changes.addChangedClassMember(classId, SAM_LOOKUP_NAME.asString())
        }
    }
}

/** Intermediate data to compute [ClasspathChanges] (see [toClasspathChanges]). */
class ChangeSet(

    /** Set of changed classes, preferably ordered by not required. */
    val changedClasses: Set<ClassId>,

    /**
     * Map from a [ClassId] to the names of changed fields/methods within that class.
     *
     * The map and sets are preferably ordered but not required.
     *
     * The [ClassId]s must not appear in [changedClasses] to avoid redundancy.
     */
    val changedClassMembers: Map<ClassId, Set<String>>
) {
    init {
        check(changedClassMembers.keys.none { it in changedClasses })
    }

    class Collector {
        private val changedClasses = mutableSetOf<ClassId>()
        private val changedClassMembers = mutableMapOf<ClassId, MutableSet<String>>()

        fun addChangedClasses(classNames: Collection<ClassId>) = changedClasses.addAll(classNames)

        fun addChangedClass(className: ClassId) = changedClasses.add(className)

        fun addChangedClassMembers(className: ClassId, memberNames: Collection<String>) {
            if (memberNames.isNotEmpty()) {
                changedClassMembers.computeIfAbsent(className) { mutableSetOf() }.addAll(memberNames)
            }
        }

        fun addChangedClassMember(className: ClassId, memberName: String) = addChangedClassMembers(className, listOf(memberName))

        fun getChanges(): ChangeSet {
            // Remove redundancy in the change set first
            changedClassMembers.keys.intersect(changedClasses).forEach { changedClassMembers.remove(it) }
            return ChangeSet(changedClasses.toSet(), changedClassMembers.toMap())
        }
    }

    fun isEmpty() = changedClasses.isEmpty() && changedClassMembers.isEmpty()

    fun toClasspathChanges(): ClasspathChanges.Available {
        val lookupSymbols = mutableSetOf<LookupSymbol>()
        val fqNames = mutableSetOf<FqName>()

        changedClasses.forEach {
            val classFqName = it.asSingleFqName()
            lookupSymbols.add(LookupSymbol(name = classFqName.shortName().asString(), scope = classFqName.parent().asString()))
            fqNames.add(classFqName)
        }

        for ((changedClass, changedClassMembers) in changedClassMembers) {
            val classFqName = changedClass.asSingleFqName()
            changedClassMembers.forEach {
                lookupSymbols.add(LookupSymbol(name = it, scope = classFqName.asString()))
            }
            fqNames.add(classFqName)
        }

        return ClasspathChanges.Available(lookupSymbols, fqNames)
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
    fun computeImpactedSet(changes: ChangeSet, previousJavaClassSnapshots: List<RegularJavaClassSnapshot>): ChangeSet {
        if (changes.isEmpty()) {
            return changes
        }

        val classIdToSubclasses = getClassIdToSubclassesMap(previousJavaClassSnapshots)

        return ChangeSet.Collector().run {
            addChangedClasses(findSubclassesInclusive(changes.changedClasses, classIdToSubclasses))
            for ((changedClass, changedClassMembers) in changes.changedClassMembers) {
                findSubclassesInclusive(setOf(changedClass), classIdToSubclasses).forEach {
                    addChangedClassMembers(it, changedClassMembers)
                }
            }
            getChanges()
        }
    }

    private fun getClassIdToSubclassesMap(classSnapshots: List<RegularJavaClassSnapshot>): LinkedHashMap<ClassId, LinkedHashSet<ClassId>> {
        val classIdToSubclasses = LinkedHashMap<ClassId, LinkedHashSet<ClassId>>()
        val classNameToClassId = classSnapshots.associate { it.className to it.classId }
        classSnapshots.forEach { classSnapshot ->
            val classId = classSnapshot.classId
            classSnapshot.supertypes
                .mapNotNull { classNameToClassId[it] } // No need to consider classes outside classSnapshots (e.g., "java/lang/Object")
                .forEach {
                    classIdToSubclasses.computeIfAbsent(it) { LinkedHashSet() }.add(classId)
                }
        }
        return classIdToSubclasses
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
