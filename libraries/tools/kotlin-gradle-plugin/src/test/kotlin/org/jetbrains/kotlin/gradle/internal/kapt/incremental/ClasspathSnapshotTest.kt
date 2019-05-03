/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ClasspathSnapshotTest {
    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    @Test
    fun testSerialization() {
        val (firstJar, lazyData) = generateLazyData(
            ClassData("first/A"), ClassData("first/B")
        )

        val snapshotDir = tmp.newFolder()
        val currentSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(snapshotDir, listOf(firstJar), setOf(lazyData))
        assertEquals(KaptClasspathChanges.Unknown, currentSnapshot.diff(UnknownSnapshot, setOf(firstJar)))
        currentSnapshot.writeToCache()

        val loadedSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.loadFrom(snapshotDir)

        val diff = loadedSnapshot.diff(currentSnapshot, setOf(firstJar)) as KaptClasspathChanges.Known
        assertEquals(emptySet<String>(), diff.names)
    }

    @Test
    fun testIncompatibleClasspaths() {
        val firstSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(File(""), listOf(File("1.jar")), emptySet())
        val secondSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(File(""), listOf(File("1.jar"), File("added.jar")), emptySet())
        assertEquals(KaptClasspathChanges.Unknown, firstSnapshot.diff(secondSnapshot, setOf(File("added.jar"))))
    }

    @Test
    fun testChangedClassesFound() {
        val (firstJar, firstLazyData) = generateLazyData(
            ClassData("first/A"),
            ClassData("first/B").also { it.withAbiDependencies("first/A") }
        )
        val firstSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(File(""), listOf(firstJar), setOf(firstLazyData))
        firstSnapshot.diff(UnknownSnapshot, setOf(firstJar))

        val (_, changedLazyData) = generateLazyData(
            ClassData("first/A", ByteArray(1)),
            ClassData("first/B").also { it.withAbiDependencies("first/A") },
            jarInput = firstJar
        )
        val changedSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(File(""), listOf(firstJar), setOf(changedLazyData))

        val diff = changedSnapshot.diff(firstSnapshot, setOf(firstJar)) as KaptClasspathChanges.Known
        assertEquals(setOf("first/A", "first/B"), diff.names)
    }

    @Test
    fun testChangedClassesAcrossEntries() {
        val (firstJar, firstLazyData) = generateLazyData(
            ClassData("first/A").also { it.withAbiDependencies("library/C") },
            ClassData("first/B").also { it.withAbiDependencies("first/A") }
        )

        val (libraryJar, libraryLazyData) = generateLazyData(ClassData("library/C"))

        val cacheDir = tmp.newFolder()
        val firstSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(
                cacheDir,
                listOf(firstJar, libraryJar),
                setOf(firstLazyData, libraryLazyData)
            )
        firstSnapshot.diff(UnknownSnapshot, setOf(firstJar, libraryJar))
        firstSnapshot.writeToCache()

        val (_, changedLazyData) = generateLazyData(ClassData("library/C", ByteArray(1)), jarInput = libraryJar)
        val changedSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(
                cacheDir,
                listOf(firstJar, libraryJar),
                setOf(firstLazyData, changedLazyData)
            )

        val diff = changedSnapshot.diff(firstSnapshot, setOf(libraryJar)) as KaptClasspathChanges.Known
        assertEquals(setOf("library/C", "first/A", "first/B"), diff.names)
    }

    private fun generateLazyData(
        vararg classData: ClassData,
        jarInput: File = tmp.newFile()
    ): Pair<File, File> {
        val data = ClasspathEntryData()
        classData.forEach {
            data.classAbiHash[it.internalName] = it.hash
            data.classDependencies[it.internalName] = ClassDependencies(it.abiDeps, it.privateDeps)
        }
        val serialized = tmp.newFile().also { data.saveTo(it) }
        val lazyData = tmp.newFile().also { LazyClasspathEntryData(jarInput, serialized).saveToFile(it) }

        return Pair(jarInput, lazyData)
    }

    private class ClassData(
        val internalName: String,
        val hash: ByteArray = ByteArray(0)
    ) {
        val abiDeps = mutableSetOf<String>()
        val privateDeps = mutableSetOf<String>()
        fun withAbiDependencies(vararg names: String) {
            abiDeps.addAll(names)
        }
    }
}