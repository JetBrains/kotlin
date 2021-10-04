/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.compileAll
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshot
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshotAll
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
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

    @Test
    abstract fun testImpactAnalysis()
}

class KotlinOnlyClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testAbiChange_changePublicMethodSignature() {
        val sourceFile = SimpleKotlinClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changePublicMethodSignature().compileAndSnapshot()
        val changes = ClasspathChangesComputer.computeClassChanges(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicFunction", scope = "com.example.SimpleKotlinClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleKotlinClass")
            ),
            fqNames = setOf("com.example.SimpleKotlinClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testNonAbiChange_changeMethodImplementation() {
        val sourceFile = SimpleKotlinClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changeMethodImplementation().compileAndSnapshot()
        val changes = ClasspathChangesComputer.computeClassChanges(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

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
                LookupSymbol(name = "modifiedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),

                // AddedClass
                LookupSymbol(name = "AddedClass", scope = "com.example"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example"),

                // Top-level properties and functions
                LookupSymbol(name = "modifiedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "modifiedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example")
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

    @Test
    override fun testImpactAnalysis() {
        val classpathSourceDir =
            File(testDataDir, "../ClasspathChangesComputerTest/testImpactAnalysis_KotlinOrJava/src/kotlin").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedProperty", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubSubClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubSubClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubSubClass")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClass",
                "com.example.SubSubClass"
            )
        ).assertEquals(changes)
    }
}

@RunWith(Parameterized::class)
class JavaOnlyClasspathChangesComputerTest(private val protoBased: Boolean) : ClasspathChangesComputerTest() {

    companion object {
        @Parameterized.Parameters(name = "protoBased={0}")
        @JvmStatic
        fun parameters() = listOf(true, false)
    }

    @Test
    override fun testAbiChange_changePublicMethodSignature() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compile().snapshot(protoBased)
        val currentSnapshot = sourceFile.changePublicMethodSignature().compile().snapshot(protoBased)
        val changes = ClasspathChangesComputer.computeClassChanges(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicMethod", scope = "com.example.SimpleJavaClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleJavaClass")
            ),
            fqNames = setOf("com.example.SimpleJavaClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testNonAbiChange_changeMethodImplementation() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compile().snapshot(protoBased)
        val currentSnapshot = sourceFile.changeMethodImplementation().compile().snapshot(protoBased)
        val changes = ClasspathChangesComputer.computeClassChanges(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(emptySet(), emptySet()).assertEquals(changes)
    }

    @Test
    override fun testVariousAbiChanges() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testVariousAbiChanges/src/java").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir, protoBased)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir, protoBased)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        Changes(
            lookupSymbols = setOf(
                // ModifiedClassUnchangedMembers
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),

                // ModifiedClassChangedMembers
                LookupSymbol(name = "modifiedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example")
            ) + if (protoBased) {
                setOf(
                    // ModifiedClassChangedMembers
                    LookupSymbol(name = "addedField", scope = "com.example.ModifiedClassChangedMembers"),
                    LookupSymbol(name = "addedMethod", scope = "com.example.ModifiedClassChangedMembers"),

                    // AddedClass
                    LookupSymbol(name = "AddedClass", scope = "com.example"),
                )
            } else {
                emptySet()
            },
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.RemovedClass"
            ) + if (protoBased) {
                setOf("com.example.AddedClass")
            } else {
                emptySet()
            }
        ).assertEquals(changes)
    }

    @Test
    override fun testImpactAnalysis() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testImpactAnalysis_KotlinOrJava/src/java").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir, protoBased)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir, protoBased)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedField", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.SubClass"),
                LookupSymbol(name = "changedField", scope = "com.example.SubSubClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.SubClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.SubSubClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubSubClass")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClass",
                "com.example.SubSubClass"
            )
        ).assertEquals(changes)
    }
}

class KotlinAndJavaClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    @Test
    fun testImpactAnalysis() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testImpactAnalysis_KotlinAndJava/src").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedProperty", scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.KotlinSubClassOfKotlinSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.JavaSubClassOfKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.KotlinSubClassOfKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.JavaSubClassOfKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.KotlinSubClassOfKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.JavaSubClassOfKotlinSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.KotlinSubClassOfJavaSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.JavaSubClassOfJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.KotlinSubClassOfJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.JavaSubClassOfJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.KotlinSubClassOfJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.JavaSubClassOfJavaSuperClass")
            ),
            fqNames = setOf(
                "com.example.ChangedKotlinSuperClass",
                "com.example.KotlinSubClassOfKotlinSuperClass",
                "com.example.JavaSubClassOfKotlinSuperClass",
                "com.example.ChangedJavaSuperClass",
                "com.example.KotlinSubClassOfJavaSuperClass",
                "com.example.JavaSubClassOfJavaSuperClass"
            )
        ).assertEquals(changes)
    }
}

private fun snapshotClasspath(classpathSourceDir: File, tmpDir: TemporaryFolder, protoBased: Boolean = true): ClasspathSnapshot {
    val classpath = mutableListOf<File>()
    val classpathEntrySnapshots = classpathSourceDir.listFiles()!!.sortedBy { it.name }.map { classpathEntrySourceDir ->
        val classFiles = compileAll(classpathEntrySourceDir, classpath, tmpDir)
        classpath.addAll(listOfNotNull(classFiles.firstOrNull()?.classRoot))

        val relativePaths = classFiles.map { it.unixStyleRelativePath }
        val classSnapshots = classFiles.snapshotAll(protoBased)
        ClasspathEntrySnapshot(
            classSnapshots = relativePaths.zip(classSnapshots).toMap(LinkedHashMap())
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
    listOfNotNull(
        compare(expected = this.lookupSymbols, actual = actual.lookupSymbols),
        compare(expected = this.fqNames, actual = actual.fqNames)
    ).also {
        if (it.isNotEmpty()) {
            fail(it.joinToString("\n"))
        }
    }
}

private fun compare(expected: Set<*>, actual: Set<*>): String? {
    return if (expected != actual) {
        "Two sets differ:\n" +
                "Elements in expected set but not in actual set: ${expected - actual}\n" +
                "Elements in actual set but not in expected set: ${actual - expected}"
    } else null
}
