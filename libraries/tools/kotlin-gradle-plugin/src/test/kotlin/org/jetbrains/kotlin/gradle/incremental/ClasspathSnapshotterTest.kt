/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
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
        assertEquals(getExpectedSnapshotFile().readText(), testClassSnapshot.toGson())
    }

    private fun getExpectedSnapshotFile() = testSourceFile.sourceFile.asFile().path.let {
        File(it.substringBeforeLast("src") + "expected-snapshot" + it.substringAfterLast("src").substringBeforeLast('.') + ".json")
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

@RunWith(Parameterized::class)
class JavaClassesClasspathSnapshotterTest(private val protoBased: Boolean) : ClasspathSnapshotTestCommon() {

    companion object {
        @Parameterized.Parameters(name = "protoBased={0}")
        @JvmStatic
        fun parameters() = listOf(true, false)
    }

    private val testSourceFile = SimpleJavaClass(tmpDir)

    private lateinit var testClassSnapshot: ClassSnapshot

    @Before
    fun setUp() {
        testClassSnapshot = testSourceFile.compile().snapshot(protoBased)
    }

    @Test
    fun `test ClassSnapshotter's result against expected snapshot`() {
        assertEquals(getExpectedSnapshotFile().readText(), testClassSnapshot.toGson())
    }

    private fun getExpectedSnapshotFile() = testSourceFile.sourceFile.asFile().path.let {
        File(
            it.substringBeforeLast("src") + "expected-snapshot" + it.substringAfterLast("src")
                .substringBeforeLast('.') + "-protoBased=$protoBased.json"
        )
    }

    @Test
    fun `test ClassSnapshotter extracts ABI info from a class`() {
        // Change public method signature
        val updatedSnapshot = testSourceFile.changePublicMethodSignature().compile().snapshot(protoBased)

        // The snapshot must change
        assertNotEquals(testClassSnapshot.toGson(), updatedSnapshot.toGson())
    }

    @Test
    fun `test ClassSnapshotter does not extract non-ABI info from a class`() {
        // Change method implementation
        val updatedSnapshot = testSourceFile.changeMethodImplementation().compile().snapshot(protoBased)

        // The snapshot must not change
        assertEquals(testClassSnapshot.toGson(), updatedSnapshot.toGson())
    }
}

class JavaClassWithNestedClassesClasspathSnapshotterTest : ClasspathSnapshotTestCommon() {

    private val testSourceFile = JavaClassWithNestedClasses(tmpDir)

    private lateinit var testClassSnapshot: ClassSnapshot

    @Before
    fun setUp() {
        testClassSnapshot = testSourceFile.compileAndSnapshotNestedClass()
    }

    private fun TestSourceFile.compileAndSnapshotNestedClass(): ClassSnapshot {
        return compileAndSnapshotAll().single {
            if (it is RegularJavaClassSnapshot) {
                it.classAbiExcludingMembers.name == testSourceFile.nestedClassToTest
            } else false
        }
    }

    @Test
    fun `test ClassSnapshotter's result against expected snapshot`() {
        val expectedSnapshotFile = File("${testDataDir.path}/expected-snapshot/java/${testSourceFile.nestedClassToTest}.json")
        assertEquals(expectedSnapshotFile.readText(), testClassSnapshot.toGson())
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
