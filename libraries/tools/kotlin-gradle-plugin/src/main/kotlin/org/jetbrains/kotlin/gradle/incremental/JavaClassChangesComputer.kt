/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.name.ClassId
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

        return ChangeSet.Collector().run {
            addChangedClasses(removedClasses)
            unchangedOrModifiedClasses.forEach {
                collectClassChanges(currentClasses[it]!!, previousClasses[it]!!, this)
            }
            getChanges()
        }
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
