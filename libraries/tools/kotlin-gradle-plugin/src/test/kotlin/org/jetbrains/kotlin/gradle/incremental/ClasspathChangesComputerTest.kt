/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

abstract class ClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    protected abstract val testSourceFile: ChangeableTestSourceFile

    private lateinit var originalSnapshot: ClassSnapshot

    @Before
    fun setUp() {
        originalSnapshot = testSourceFile.compileAndSnapshot()
    }

    // TODO Add more test cases:
    //   - private/non-private fields
    //   - inline functions
    //   - changing supertype by adding somethings that changes/does not change the supertype ABI
    //   - adding an annotation

    @Test
    fun testCollectClassChanges_changedPublicMethodSignature() {
        val updatedSnapshot = testSourceFile.changePublicMethodSignature().compileAndSnapshot()
        val dirtyData = ClasspathChangesComputer.collectKotlinClassChanges(
            updatedSnapshot as KotlinClassSnapshot,
            originalSnapshot as KotlinClassSnapshot
        )

        assertEquals(
            DirtyData(
                dirtyLookupSymbols = setOf(
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleKotlinClass"),
                    LookupSymbol(name = "publicMethod", scope = "com.example.SimpleKotlinClass"),
                    LookupSymbol(name = "changedPublicMethod", scope = "com.example.SimpleKotlinClass")
                ),
                dirtyClassesFqNames = setOf(FqName("com.example.SimpleKotlinClass")),
                dirtyClassesFqNamesForceRecompile = emptySet()
            ),
            dirtyData
        )
    }

    @Test
    fun testCollectClassChanges_changedMethodImplementation() {
        val updatedSnapshot = testSourceFile.changeMethodImplementation().compileAndSnapshot()
        val dirtyData = ClasspathChangesComputer.collectKotlinClassChanges(
            updatedSnapshot as KotlinClassSnapshot,
            originalSnapshot as KotlinClassSnapshot
        )

        assertEquals(DirtyData(emptySet(), emptySet(), emptySet()), dirtyData)
    }
}

class KotlinClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {
    override val testSourceFile = SimpleKotlinClass(tmpDir)
}
