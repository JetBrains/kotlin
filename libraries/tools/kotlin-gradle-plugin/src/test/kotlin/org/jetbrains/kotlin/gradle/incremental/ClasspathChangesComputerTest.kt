/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.compileAll
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.fail

abstract class ClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    // TODO Add more test cases:
    //   - private/non-private fields
    //   - inline functions
    //   - changing supertype by adding somethings that changes/does not change the supertype ABI
    //   - adding an annotation

    @Test
    abstract fun testAbiChange_changePublicMethodSignature()

    @Test
    abstract fun testNonAbiChange_changeMethodImplementation()

    @Test
    abstract fun testVariousAbiChanges()
}

class KotlinClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testAbiChange_changePublicMethodSignature() {
        val sourceFile = SimpleKotlinClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changePublicMethodSignature().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleKotlinClass"),
                LookupSymbol(name = "changedPublicMethod", scope = "com.example.SimpleKotlinClass"),
                LookupSymbol(name = "publicMethod", scope = "com.example.SimpleKotlinClass")
            ),
            fqNames = setOf("com.example.SimpleKotlinClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testNonAbiChange_changeMethodImplementation() {
        val sourceFile = SimpleKotlinClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changeMethodImplementation().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(emptySet(), emptySet()).assertEquals(changes)
    }

    @Test
    override fun testVariousAbiChanges() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testVariousAbiChanges/src/kotlin").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        Changes(
            lookupSymbols = setOf(
                // ModifiedClassUnchangedMembers
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),

                // ModifiedClassChangedMembers
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedFunction", scope = "com.example.ModifiedClassChangedMembers"),

                // AddedClass
                LookupSymbol(name = "AddedClass", scope = "com.example"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example"),

                // Top-level properties and functions
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example"),
                LookupSymbol(name = "modifiedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "modifiedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelFunction", scope = "com.example"),
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass",
                "com.example"
            )
        ).assertEquals(changes)
    }
}

class JavaClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testAbiChange_changePublicMethodSignature() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changePublicMethodSignature().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleJavaClass"),
                LookupSymbol(name = "changedPublicMethod", scope = "com.example.SimpleJavaClass"),
                LookupSymbol(name = "publicMethod", scope = "com.example.SimpleJavaClass")
            ),
            fqNames = setOf("com.example.SimpleJavaClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testNonAbiChange_changeMethodImplementation() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changeMethodImplementation().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(emptySet(), emptySet()).assertEquals(changes)
    }

    @Test
    override fun testVariousAbiChanges() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testVariousAbiChanges/src/java").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        Changes(
            lookupSymbols = setOf(
                // ModifiedClassUnchangedMembers
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),

                // ModifiedClassChangedMembers
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedMethod", scope = "com.example.ModifiedClassChangedMembers"),

                // AddedClass
                LookupSymbol(name = "AddedClass", scope = "com.example"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.AddedClass"),
                LookupSymbol(name = "someField", scope = "com.example.AddedClass"),
                LookupSymbol(name = "<init>", scope = "com.example.AddedClass"),
                LookupSymbol(name = "someMethod", scope = "com.example.AddedClass"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example"),
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass",
            )
        ).assertEquals(changes)
    }
}

private fun snapshotClasspath(classpathSourceDir: File, tmpDir: TemporaryFolder): ClasspathSnapshot {
    val classpathEntrySnapshots = classpathSourceDir.listFiles()!!.map { classpathEntrySourceDir ->
        val classFiles = compileAll(classpathEntrySourceDir, tmpDir)
        ClasspathEntrySnapshot(
            classSnapshots = classFiles.associateTo(LinkedHashMap()) { it.unixStyleRelativePath to it.snapshot() }
        )
    }
    return ClasspathSnapshot(classpathEntrySnapshots)
}

/** Adapted version of [ClasspathChanges.Available] for readability in this test. */
private data class Changes(val lookupSymbols: Set<LookupSymbol>, val fqNames: Set<String>)

private fun ClasspathChanges.normalize(): Changes {
    this as ClasspathChanges.Available
    return Changes(lookupSymbols, fqNames.map { it.asString() }.toSet())
}

private fun Changes.assertEquals(actual: Changes) {
    assertSetEquals(expected = this.lookupSymbols, actual = actual.lookupSymbols)
    assertSetEquals(expected = this.fqNames, actual = actual.fqNames)
}

private fun assertSetEquals(expected: Set<*>, actual: Set<*>) {
    if (expected != actual) {
        fail(
            "Two sets differ:\n" +
                    "Elements in expected set but not in actual set: ${expected - actual}\n" +
                    "Elements in actual set but not in expected set: ${actual - expected}"
        )
    }
}
