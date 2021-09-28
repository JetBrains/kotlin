/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incremental

import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.*
import org.jetbrains.kotlin.gradle.incremental.ClasspathSnapshotTestCommon.Util.snapshot
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class ClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    // TODO Add more test cases:
    //   - private/non-private fields
    //   - inline functions
    //   - changing supertype by adding somethings that changes/does not change the supertype ABI
    //   - adding an annotation

    @Test
    abstract fun testSingleClass_changePublicMethodSignature()

    @Test
    abstract fun testSingleClass_changeMethodImplementation()

    @Test
    abstract fun testMultipleClasses()
}

class KotlinClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testSingleClass_changePublicMethodSignature() {
        val sourceFile = SimpleKotlinClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changePublicMethodSignature().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        assertEquals(
            Changes(
                lookupSymbols = setOf(
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleKotlinClass"),
                    LookupSymbol(name = "changedPublicMethod", scope = "com.example.SimpleKotlinClass"),
                    LookupSymbol(name = "publicMethod", scope = "com.example.SimpleKotlinClass")
                ),
                fqNames = setOf(
                    FqName("com.example.SimpleKotlinClass")
                ),
            ),
            changes
        )
    }

    @Test
    override fun testSingleClass_changeMethodImplementation() {
        val sourceFile = SimpleKotlinClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changeMethodImplementation().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        assertEquals(Changes(emptySet(), emptySet()), changes)
    }

    @Test
    override fun testMultipleClasses() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testMultipleClasses/src/kotlin").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        assertEquals(
            Changes(
                lookupSymbols = setOf(
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.B"),
                    LookupSymbol(name = "b2", scope = "com.example.B"),
                    LookupSymbol(name = "b3", scope = "com.example.B"),
                    LookupSymbol(name = "b4", scope = "com.example.B"),
                    LookupSymbol(name = "C", scope = "com.example"),
                    LookupSymbol(name = "D", scope = "com.example"),
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example"),
                    LookupSymbol(name = "topLevelFuncB", scope = "com.example"),
                    LookupSymbol(name = "topLevelFuncC", scope = "com.example"),
                    LookupSymbol(name = "topLevelFuncD", scope = "com.example"),
                    LookupSymbol(name = "topLevelFuncInCKtMovedToDKt", scope = "com.example"),
                    LookupSymbol(name = "CKt", scope = "com.example"),
                ),
                fqNames = setOf(
                    FqName("com.example.B"),
                    FqName("com.example.C"),
                    FqName("com.example.D"),
                    FqName("com.example"),
                    FqName("com.example.CKt"),
                )
            ),
            changes
        )
    }
}

class JavaClassesClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testSingleClass_changePublicMethodSignature() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changePublicMethodSignature().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        assertEquals(
            Changes(
                lookupSymbols = setOf(
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SimpleJavaClass"),
                    LookupSymbol(name = "changedPublicMethod", scope = "com.example.SimpleJavaClass"),
                    LookupSymbol(name = "publicMethod", scope = "com.example.SimpleJavaClass")
                ),
                fqNames = setOf(
                    FqName("com.example.SimpleJavaClass")
                ),
            ),
            changes
        )
    }

    @Test
    override fun testSingleClass_changeMethodImplementation() {
        val sourceFile = SimpleJavaClass(tmpDir)
        val previousSnapshot = sourceFile.compileAndSnapshot()
        val currentSnapshot = sourceFile.changeMethodImplementation().compileAndSnapshot()
        val changes = ClasspathChangesComputer.compute(listOf(currentSnapshot), listOf(previousSnapshot)).normalize()

        assertEquals(Changes(emptySet(), emptySet()), changes)
    }

    @Test
    override fun testMultipleClasses() {
        val classpathSourceDir = File(testDataDir, "../ClasspathChangesComputerTest/testMultipleClasses/src/java").canonicalFile
        val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir)
        val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir)
        val changes = ClasspathChangesComputer.compute(currentSnapshot, previousSnapshot).normalize()

        assertEquals(
            Changes(
                lookupSymbols = setOf(
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.B"),
                    LookupSymbol(name = "b2", scope = "com.example.B"),
                    LookupSymbol(name = "b3", scope = "com.example.B"),
                    LookupSymbol(name = "b4", scope = "com.example.B"),
                    LookupSymbol(name = "C", scope = "com.example"),
                    LookupSymbol(name = "D", scope = "com.example"),
                    LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.D"),
                    LookupSymbol(name = "<init>", scope = "com.example.D"),
                    LookupSymbol(name = "d", scope = "com.example.D")
                ),
                fqNames = setOf(
                    FqName("com.example.B"),
                    FqName("com.example.C"),
                    FqName("com.example.D")
                )
            ),
            changes
        )
    }
}

private fun snapshotClasspath(classpathSourceDir: File, tmpDir: TemporaryFolder): ClasspathSnapshot {
    val classpathEntrySnapshots = classpathSourceDir.listFiles()!!.map { classpathEntrySourceDir ->
        val relativePathsInDir = classpathEntrySourceDir.walk()
            .filter { it.extension == "kt" || it.extension == "java" }
            .map { file -> file.toRelativeString(classpathEntrySourceDir) }
            .sortedBy { it }
        val sourceFiles = relativePathsInDir.map { relativePath ->
            if (relativePath.endsWith(".kt")) {
                val preCompiledClassFilesRoot = classpathEntrySourceDir.path.let {
                    File(it.substringBeforeLast("src") + "classes" + it.substringAfterLast("src"))
                }.also { check(it.exists()) }
                KotlinSourceFile(
                    classpathEntrySourceDir, relativePath,
                    preCompiledClassFiles = listOf(
                        ClassFile(preCompiledClassFilesRoot, relativePath.replace(".kt", ".class")),
                        ClassFile(preCompiledClassFilesRoot, relativePath.replace(".kt", "Kt.class"))
                    ).filter { File(it.classRoot, it.unixStyleRelativePath).exists() }
                )
            } else {
                SourceFile(classpathEntrySourceDir, relativePath)
            }
        }
        val classFiles = sourceFiles.flatMap { TestSourceFile(it, tmpDir).compileAll() }
        ClasspathEntrySnapshot(
            classSnapshots = classFiles.map { it.unixStyleRelativePath to it.snapshot() }.toMap(LinkedHashMap())
        )
    }
    return ClasspathSnapshot(classpathEntrySnapshots)
}

/** Adapted version of [ClasspathChanges.Available] for readability in this test. */
private data class Changes(private val lookupSymbols: Set<LookupSymbol>, private val fqNames: Set<FqName>)

private fun ClasspathChanges.normalize(): Changes {
    this as ClasspathChanges.Available
    return Changes(lookupSymbols, fqNames)
}
