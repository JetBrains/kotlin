/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME

/** Computes [SymbolChanges] between two lists of [JavaClassSnapshot]s .*/
object JavaClassChangesComputer {

    /**
     * Computes [SymbolChanges] between two lists of [JavaClassSnapshot]s.
     *
     * Each list must not contain duplicate classes (having the same [JvmClassName]).
     */
    fun compute(
        currentJavaClassSnapshots: List<RegularJavaClassSnapshot>,
        previousJavaClassSnapshots: List<RegularJavaClassSnapshot>
    ): SymbolChanges {
        val currentClasses: Map<JvmClassName, RegularJavaClassSnapshot> = currentJavaClassSnapshots.associateBy { it.getJvmClassName() }
        val previousClasses: Map<JvmClassName, RegularJavaClassSnapshot> = previousJavaClassSnapshots.associateBy { it.getJvmClassName() }

        val addedClasses = currentClasses.keys - previousClasses.keys
        val removedClasses = previousClasses.keys - currentClasses.keys
        val unchangedOrModifiedClasses = currentClasses.keys - addedClasses

        val changes = SymbolChangesCollector()
        with(changes) {
            addedClasses.forEach { addClass(it) }
            removedClasses.forEach { addClass(it) }
            unchangedOrModifiedClasses.forEach {
                collectClassChanges(currentClasses[it]!!, previousClasses[it]!!, changes)
            }
        }

        return changes.getSymbolChanges()
    }

    /**
     * Collects changes between two [JavaClassSnapshot]s.
     *
     * The two classes must have the same [JvmClassName].
     */
    private fun collectClassChanges(
        currentClassSnapshot: RegularJavaClassSnapshot,
        previousClassSnapshot: RegularJavaClassSnapshot,
        changes: SymbolChangesCollector
    ) {
        val jvmClassName = currentClassSnapshot.getJvmClassName().also { check(it == previousClassSnapshot.getJvmClassName()) }
        if (currentClassSnapshot.classAbiExcludingMembers.abiHash != previousClassSnapshot.classAbiExcludingMembers.abiHash) {
            changes.addClass(jvmClassName)
        }
        collectClassMemberChanges(jvmClassName, currentClassSnapshot.fieldsAbi, previousClassSnapshot.fieldsAbi, changes)
        collectClassMemberChanges(jvmClassName, currentClassSnapshot.methodsAbi, previousClassSnapshot.methodsAbi, changes)
    }

    /** Collects changes between two lists of Java fields/methods within the given class. */
    private fun collectClassMemberChanges(
        jvmClassName: JvmClassName,
        currentMemberSnapshots: List<AbiSnapshot>,
        previousMemberSnapshots: List<AbiSnapshot>,
        changes: SymbolChangesCollector
    ) {
        val currentMemberHashes: Map<Long, AbiSnapshot> = currentMemberSnapshots.associateBy { it.abiHash }
        val previousMemberHashes: Map<Long, AbiSnapshot> = previousMemberSnapshots.associateBy { it.abiHash }

        val addedMembers = currentMemberHashes.keys - previousMemberHashes.keys
        val removedMembers = previousMemberHashes.keys - currentMemberHashes.keys

        // Note:
        //   - Modified members will appear in both addedMembers and removedMembers.
        //   - Multiple members within a class may have the same name (but never the same signature (name + desc) or ABI hash).
        //   - It's okay to add the same lookup symbol multiple times.
        with(changes) {
            addClassMembers(addedMembers.map { currentMemberHashes[it]!!.name }, jvmClassName)
            addClassMembers(removedMembers.map { previousMemberHashes[it]!!.name }, jvmClassName)

            // TODO: Check whether the condition to add SAM_LOOKUP_NAME below is too broad, and correct it if necessary.
            if (addedMembers.isNotEmpty() || removedMembers.isNotEmpty()) {
                addClassMember(SAM_LOOKUP_NAME.asString(), jvmClassName)
            }
        }
    }
}

private fun RegularJavaClassSnapshot.getJvmClassName(): JvmClassName = JvmClassName.byInternalName(classAbiExcludingMembers.name)

/** Intermediate data structure to compute [ClasspathChanges]. */
class SymbolChanges(val lookupSymbols: Set<LookupSymbol>, val fqNames: Set<FqName>)

/** Helper class to collect [SymbolChanges]. */
private class SymbolChangesCollector {
    private val lookupSymbols = mutableSetOf<LookupSymbol>()
    private val fqNames = mutableSetOf<FqName>()

    fun addClassMember(memberName: String, jvmClassName: JvmClassName) = addClassMembers(listOf(memberName), jvmClassName)

    fun addClassMembers(memberNames: List<String>, jvmClassName: JvmClassName) {
        if (memberNames.isEmpty()) {
            return
        }
        val classFqName = jvmClassName.getClassFqName()
        memberNames.forEach {
            lookupSymbols.add(LookupSymbol(name = it, scope = classFqName.asString()))
        }
        fqNames.add(classFqName)
    }

    fun addClass(jvmClassName: JvmClassName) {
        val classFqName = jvmClassName.getClassFqName()
        lookupSymbols.add(LookupSymbol(name = classFqName.shortName().asString(), scope = classFqName.parent().asString()))
        fqNames.add(classFqName)
    }

    // FIXME: `JvmClassName` cannot be reliably converted to `FqName` (see `JvmClassName.fqNameForClassNameWithoutDollars`).
    // To do this correctly, we will need to get `FqName` from a `ClassId`, which is richer than `JvmClassName`. Note that creating a
    // `ClassId` will require more info than just the current class as it needs to resolve the `ClassId`s of outer classes recursively.
    private fun JvmClassName.getClassFqName(): FqName = fqNameForClassNameWithoutDollars

    fun getSymbolChanges() = SymbolChanges(lookupSymbols.toSet(), fqNames.toSet())
}
