/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SpellCheckingInspection")

package org.jetbrains.kotlin.gradle.incremental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.io.File

abstract class ClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    protected abstract val testSourceFile: ChangeableTestSourceFile

    private lateinit var testClassSnapshot: ClassSnapshot

    @Before
    fun setUp() {
        testClassSnapshot = testSourceFile.compileAndSnapshot()
    }

    @Test
    fun `test ClassSnapshotter's result against expected snapshot`() {
        val expectedSnapshot = File("${testSourceFile.sourceFile.asFile().path.substringBeforeLast('.')}-expected-snapshot.json").readText()
        assertEquals(expectedSnapshot, testClassSnapshot.toGson())
    }

    @Test
    fun `test ClassSnapshotter extracts ABI info from a class`() {
        // Change public method signature
        val updatedSnapshot = testSourceFile.changePublicMethodSignature().compileAndSnapshot()

        // The snapshot must change
        assertNotEquals(testClassSnapshot.toGson(), updatedSnapshot.toGson())
    }

    @Test
    fun `test ClassSnapshotter does not extract non-ABI info from a class`() {
        // Change method implementation
        val updatedSnapshot = testSourceFile.changeMethodImplementation().compileAndSnapshot()

        // The snapshot must not change
        assertEquals(testClassSnapshot.toGson(), updatedSnapshot.toGson())
    }
}

class KotlinClassesClasspathSnapshotterTest : ClasspathSnapshotterTest() {
    override val testSourceFile = SimpleKotlinClass(tmpDir)
}

class JavaClassesClasspathSnapshotterTest : ClasspathSnapshotterTest() {
    override val testSourceFile = SimpleJavaClass(tmpDir)
}

class JavaClassWithNestedClassesClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    private val testSourceFile = JavaClassWithNestedClasses(tmpDir)

    private lateinit var testClassSnapshot: ClassSnapshot

    @Before
    fun setUp() {
        testClassSnapshot = testSourceFile.compileAndSnapshotNestedClass()
    }

    private fun TestSourceFile.compileAndSnapshotNestedClass(): ClassSnapshot {
        return compileAndSnapshotAll()[5].also {
            assertEquals(
                testSourceFile.nestedClassToTest,
                (it as RegularJavaClassSnapshot).serializedJavaClass.classId.asString().replace('.', '$')
            )
        }
    }

    @Test
    fun `test ClassSnapshotter's result against expected snapshot`() {
        val expectedSnapshot =
            File("${testDataDir.path}/src/original/${testSourceFile.nestedClassToTest}-expected-snapshot.json").readText()
        assertEquals(expectedSnapshot, testClassSnapshot.toGson())
    }

    @Test
    fun `test ClassSnapshotter extracts ABI info from a class`() {
        val updatedSnapshot = testSourceFile.changePublicMethodSignature().compileAndSnapshotNestedClass()
        assertNotEquals(testClassSnapshot.toGson(), updatedSnapshot.toGson())
    }

    @Test
    fun `test ClassSnapshotter does not extract non-ABI info from a class`() {
        val updatedSnapshot = testSourceFile.changeMethodImplementation().compileAndSnapshotNestedClass()
        assertEquals(testClassSnapshot.toGson(), updatedSnapshot.toGson())
    }
}
