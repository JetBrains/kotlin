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

class KotlinClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testAbiChange_changePublicMethodSignature() {
        val sourceFile = SimpleKotlinClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changePublicMethodSignature().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedPublicMethod", scope = "com.example.SimpleKotlinClass"),
                LookupSymbol(name = "publicMethod", scope = "com.example.SimpleKotlinClass"),
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
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testImpactAnalysis/src/kotlin").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "someProperty", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "someProperty", scope = "com.example.SubClassSameModule"),
                LookupSymbol(name = "someProperty", scope = "com.example.SubClassDifferentModule"),
                LookupSymbol(name = "someFunction", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "someFunction", scope = "com.example.SubClassSameModule"),
                LookupSymbol(name = "someFunction", scope = "com.example.SubClassDifferentModule"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassSameModule"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassDifferentModule")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClassSameModule",
                "com.example.SubClassDifferentModule"
            )
        ).assertEquals(changes)
    }
}

@RunWith(Parameterized::class)
class JavaClassesClasspathChangesComputerTest(private val protoBased: Boolean) : ClasspathChangesComputerTest() {

    companion object {
        @Parameterized.Parameters(name = "protoBased={0}")
        @JvmStatic
        fun parameters() = listOf(true, false)
    }

    private fun computeClasspathChanges(
        currentClasspathSnapshot: ClasspathSnapshot,
        previousClasspathSnapshot: ClasspathSnapshot
    ): Changes {
        return if (protoBased) {
            ClasspathChangesComputer.compute(currentClasspathSnapshot, previousClasspathSnapshot).normalize()
        } else {
            @Suppress("UNCHECKED_CAST")
            JavaClassChangesComputer.compute(
                currentClasspathSnapshot.classpathEntrySnapshots.flatMap { it.classSnapshots.values } as List<RegularJavaClassSnapshot>,
                previousClasspathSnapshot.classpathEntrySnapshots.flatMap { it.classSnapshots.values } as List<RegularJavaClassSnapshot>
            ).normalize()
        }
    }

    private fun computeClassChanges(currentClassSnapshots: List<ClassSnapshot>, previousClassSnapshots: List<ClassSnapshot>): Changes {
        return if (protoBased) {
            ClasspathChangesComputer.compute(currentClassSnapshots, previousClassSnapshots).normalize()
        } else {
            @Suppress("UNCHECKED_CAST")
            JavaClassChangesComputer.compute(
                currentClassSnapshots as List<RegularJavaClassSnapshot>,
                previousClassSnapshots as List<RegularJavaClassSnapshot>
            ).normalize()
        }
    }

    @Test
    override fun testAbiChange_changePublicMethodSignature() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compile().snapshot(protoBased)
        val currentSnapshot = sourceFile.changePublicMethodSignature().compile().snapshot(protoBased)
        val changes = computeClassChanges(listOf(currentSnapshot), listOf(previousSnapshot))

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicMethod", scope = "com.example.SimpleJavaClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleJavaClass")
            ) + if (protoBased) {
                setOf(LookupSymbol(name = "changedPublicMethod", scope = "com.example.SimpleJavaClass"))
            } else {
                emptySet()
            },
            fqNames = setOf("com.example.SimpleJavaClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testNonAbiChange_changeMethodImplementation() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compile().snapshot(protoBased)
        val currentSnapshot = sourceFile.changeMethodImplementation().compile().snapshot(protoBased)
        val changes = computeClassChanges(listOf(currentSnapshot), listOf(previousSnapshot))

        Changes(emptySet(), emptySet()).assertEquals(changes)
    }

    @Test
    override fun testVariousAbiChanges() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testVariousAbiChanges/src/java").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir, protoBased)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir, protoBased)
        val changes = computeClasspathChanges(currentSnapshot, previousSnapshot)

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
                    LookupSymbol(name = "someField", scope = "com.example.AddedClass"),
                    LookupSymbol(name = "<init>", scope = "com.example.AddedClass"),
                    LookupSymbol(name = "someMethod", scope = "com.example.AddedClass"),
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.AddedClass")
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
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testImpactAnalysis/src/java").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir, protoBased)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir, protoBased)
        val changes = computeClasspathChanges(currentSnapshot, previousSnapshot)

        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "someField", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "someField", scope = "com.example.SubClassSameModule"),
                LookupSymbol(name = "someField", scope = "com.example.SubClassDifferentModule"),
                LookupSymbol(name = "someMethod", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "someMethod", scope = "com.example.SubClassSameModule"),
                LookupSymbol(name = "someMethod", scope = "com.example.SubClassDifferentModule"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassSameModule"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassDifferentModule")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClassSameModule",
                "com.example.SubClassDifferentModule"
            )
        ).assertEquals(changes)
    }
}

private fun snapshotClasspath(classpathSourceDir: File, tmpDir: TemporaryFolder, protoBased: Boolean = true): ClasspathSnapshot {
    val classpath = mutableListOf<File>()
    val classpathEntrySnapshots = classpathSourceDir.listFiles()!!.sortedBy { it.name }.map { classpathEntrySourceDir ->
        val classFiles = compileAll(classpathEntrySourceDir, classpath, tmpDir)
        classFiles.firstOrNull()?.let {
            classpath.add(it.classRoot)
        }

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

private fun ChangeSet.normalize(): Changes = toClasspathChanges().normalize()

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
