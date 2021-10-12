/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.gradle.incremental.ImpactAnalysis.computeImpactedSet
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME

/** Computes [ChangeSet] between two lists of [JavaClassSnapshot]s .*/
object JavaClassChangesComputer {

    /**
     * Computes [ChangeSet] between two lists of [JavaClassSnapshot]s.
     *
     * Each list must not contain duplicate classes (having the same [JvmClassName]).
     */
    fun compute(
        currentJavaClassSnapshots: List<RegularJavaClassSnapshot>,
        previousJavaClassSnapshots: List<RegularJavaClassSnapshot>
    ): ChangeSet {
        val currentClasses: Map<String, RegularJavaClassSnapshot> = currentJavaClassSnapshots.associateBy { it.getClassName() }
        val previousClasses: Map<String, RegularJavaClassSnapshot> = previousJavaClassSnapshots.associateBy { it.getClassName() }

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
     * The two classes must have the same [JvmClassName].
     */
    private fun collectClassChanges(
        currentClassSnapshot: RegularJavaClassSnapshot,
        previousClassSnapshot: RegularJavaClassSnapshot,
        changes: ChangeSet.Collector
    ) {
        val className = currentClassSnapshot.getClassName().also { check(it == previousClassSnapshot.getClassName()) }
        if (currentClassSnapshot.classAbiExcludingMembers.abiHash != previousClassSnapshot.classAbiExcludingMembers.abiHash) {
            changes.addChangedClass(className)
        } else {
            collectClassMemberChanges(className, currentClassSnapshot.fieldsAbi, previousClassSnapshot.fieldsAbi, changes)
            collectClassMemberChanges(className, currentClassSnapshot.methodsAbi, previousClassSnapshot.methodsAbi, changes)
        }
    }

    /** Collects changes between two lists of fields/methods within a class. */
    private fun collectClassMemberChanges(
        className: String,
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
        changes.addChangedClassMembers(className, removedMembers.map { previousMemberHashes[it]!!.name })

        // TODO: Check whether the condition to add SAM_LOOKUP_NAME below is too broad, and correct it if necessary.
        // Currently, it matches the logic in ChangesCollector.getDirtyData in buildUtil.kt.
        if (addedMembers.isNotEmpty() || removedMembers.isNotEmpty()) {
            changes.addChangedClassMember(className, SAM_LOOKUP_NAME.asString())
        }
    }
}

/** Intermediate data to compute [ClasspathChanges] (see [toClasspathChanges]). */
class ChangeSet(

    /** Set of changed classes, preferably ordered by not required. */
    val changedClasses: Set<String>,

    /**
     * Map from a class name to the names of changed fields/methods within that class. The map and sets are preferably ordered but not
     * required.
     *
     * The class names must not appear in [changedClasses] to avoid redundancy.
     */
    val changedClassMembers: Map<String, Set<String>>
) {
    init {
        check(changedClasses.intersect(changedClassMembers.keys).isEmpty())
    }

    class Collector {
        private val changedClasses = mutableSetOf<String>()
        private val changedClassMembers = mutableMapOf<String, MutableSet<String>>()

        fun addChangedClasses(classNames: Collection<String>) = changedClasses.addAll(classNames)

        fun addChangedClass(className: String) = changedClasses.add(className)

        fun addChangedClassMembers(className: String, memberNames: Collection<String>) {
            if (memberNames.isNotEmpty()) {
                changedClassMembers.computeIfAbsent(className) { mutableSetOf() }.addAll(memberNames)
            }
        }

        fun addChangedClassMember(className: String, memberName: String) = addChangedClassMembers(className, listOf(memberName))

        fun getChanges(): ChangeSet {
            // Remove redundancy in the change set first
            changedClasses.forEach { changedClassMembers.remove(it) }
            return ChangeSet(changedClasses.toSet(), changedClassMembers.toMap())
        }
    }

    fun isEmpty() = changedClasses.isEmpty() && changedClassMembers.isEmpty()

    fun toClasspathChanges(): ClasspathChanges {
        val lookupSymbols = mutableSetOf<LookupSymbol>()
        val fqNames = mutableSetOf<FqName>()

        changedClasses.forEach {
            val classFqName = JvmClassName.byInternalName(it).getClassFqName()
            lookupSymbols.add(LookupSymbol(name = classFqName.shortName().asString(), scope = classFqName.parent().asString()))
            fqNames.add(classFqName)
        }

        for ((changedClass, changedClassMembers) in changedClassMembers) {
            val classFqName = JvmClassName.byInternalName(changedClass).getClassFqName()
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

        val classNameToSubclasses = getClassNameToSubclassesMap(previousJavaClassSnapshots)

        return ChangeSet.Collector().run {
            addChangedClasses(findSubclassesInclusive(changes.changedClasses, classNameToSubclasses))
            for ((changedClass, changedClassMembers) in changes.changedClassMembers) {
                findSubclassesInclusive(setOf(changedClass), classNameToSubclasses).forEach {
                    addChangedClassMembers(it, changedClassMembers)
                }
            }
            getChanges()
        }
    }

    private fun getClassNameToSubclassesMap(classSnapshots: List<RegularJavaClassSnapshot>): LinkedHashMap<String, LinkedHashSet<String>> {
        val classNameToSubclasses = LinkedHashMap<String, LinkedHashSet<String>>()
        val allClasses = classSnapshots.map { it.getClassName() }.toSet()
        classSnapshots.forEach { classSnapshot ->
            val className = classSnapshot.getClassName()
            classSnapshot.supertypes
                .filter { it in allClasses } // No need to consider classes outside allClasses (e.g., "java/lang/Object")
                .forEach {
                    classNameToSubclasses.computeIfAbsent(it) { LinkedHashSet() }.add(className)
                }
        }
        return classNameToSubclasses
    }

    /** Finds subclasses of the given classes. The return set includes both the given classes and their subclasses. */
    private fun findSubclassesInclusive(classNames: Set<String>, classNameToSubclasses: Map<String, Set<String>>): Set<String> {
        val toVisitClasses = classNames.toMutableSet()
        val visitedClasses = mutableSetOf<String>()
        while (toVisitClasses.isNotEmpty()) {
            val nextToVisit = mutableSetOf<String>()
            toVisitClasses.forEach {
                nextToVisit.addAll(classNameToSubclasses[it] ?: emptyList())
            }
            visitedClasses.addAll(toVisitClasses)
            toVisitClasses.clear()
            toVisitClasses.addAll(nextToVisit - visitedClasses)
        }
        return visitedClasses
    }
}

private fun RegularJavaClassSnapshot.getClassName(): String = classAbiExcludingMembers.name

// FIXME: `JvmClassName` cannot be reliably converted to `FqName` (see `JvmClassName.fqNameForClassNameWithoutDollars`).
// To do this correctly, we will need to get `FqName` from a `ClassId`, which is richer than `JvmClassName`. Note that creating a `ClassId`
// will require more info than just the current class as it needs to resolve the `ClassId`s of outer classes recursively.
private fun JvmClassName.getClassFqName(): FqName = fqNameForClassNameWithoutDollars
