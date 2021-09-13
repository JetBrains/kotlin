/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.ClassId
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

    /** Adapted version of [ClasspathChanges.Available] for readability in this test. */
    private data class Changes(private val lookupSymbols: Set<LookupSymbol>, private val fqNames: Set<FqName>)

    private fun computeClassChanges(current: ClassSnapshot, previous: ClassSnapshot): Changes {
        val classChanges =
            ClasspathChangesComputer.compute(current.toClasspathSnapshot(), previous.toClasspathSnapshot()) as ClasspathChanges.Available
        return Changes(HashSet(classChanges.lookupSymbols), HashSet(classChanges.fqNames))
    }

    private fun ClassSnapshot.toClasspathSnapshot(): ClasspathSnapshot {
        return ClasspathSnapshot(
            classpathEntrySnapshots = listOf(
                ClasspathEntrySnapshot(
                    LinkedHashMap<String, ClassSnapshot>(1).also {
                        it[getClassId()!!.getUnixStyleRelativePath()] = this
                    })
            )
        )
    }

    private fun ClassSnapshot.getClassId(): ClassId? {
        return when (this) {
            is KotlinClassSnapshot -> classInfo.classId
            is RegularJavaClassSnapshot -> serializedJavaClass.classId
            is EmptyJavaClassSnapshot, is ContentHashJavaClassSnapshot -> null
        }
    }

    private fun ClassId.getUnixStyleRelativePath() = asString().replace('.', '$') + ".class"

    /**
     * Returns the [FqName] of the class in this source file (e.g., "com/example/Foo$Bar.kt" or
     * "com/example/Foo$Bar.java" has [FqName] "com.example.Foo.Bar").
     *
     * This source file must contain only 1 class.
     */
    private fun SourceFile.getClassFqName() =
        FqName(unixStyleRelativePath.substringBeforeLast('.').replace('/', '.').replace('$', '.'))

    // TODO Add more test cases:
    //   - private/non-private fields
    //   - inline functions
    //   - changing supertype by adding somethings that changes/does not change the supertype ABI
    //   - adding an annotation

    @Test
    fun testComputeClassChanges_changedPublicMethodSignature() {
        val updatedSnapshot = testSourceFile.changePublicMethodSignature().compileAndSnapshot()
        val classChanges = computeClassChanges(updatedSnapshot, originalSnapshot)

        val testClassFqName = testSourceFile.sourceFile.getClassFqName()
        assertEquals(
            Changes(
                lookupSymbols = setOf(
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = testClassFqName.asString()),
                    LookupSymbol(name = "changedPublicMethod", scope = testClassFqName.asString()),
                    LookupSymbol(name = "publicMethod", scope = testClassFqName.asString())
                ),
                fqNames = setOf(testClassFqName),
            ),
            classChanges
        )
    }

    @Test
    fun testComputeClassChanges_changedMethodImplementation() {
        val updatedSnapshot = testSourceFile.changeMethodImplementation().compileAndSnapshot()
        val classChanges = computeClassChanges(updatedSnapshot, originalSnapshot)

        assertEquals(Changes(emptySet(), emptySet()), classChanges)
    }
}

class KotlinClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {
    override val testSourceFile = SimpleKotlinClass(tmpDir)
}

class JavaClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {
    override val testSourceFile = SimpleJavaClass(tmpDir)
}
