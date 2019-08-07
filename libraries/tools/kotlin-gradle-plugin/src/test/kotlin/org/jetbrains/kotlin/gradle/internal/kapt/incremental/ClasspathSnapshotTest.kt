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
        val data = generateStructureData(
            ClassData("first/A"), ClassData("first/B")
        )

        val snapshotDir = tmp.newFolder()
        val currentSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(snapshotDir, listOf(), listOf(), setOf(data))
        assertEquals(KaptClasspathChanges.Unknown, currentSnapshot.diff(UnknownSnapshot, setOf(data)))
        currentSnapshot.writeToCache()

        val loadedSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.loadFrom(snapshotDir)

        val diff = loadedSnapshot.diff(currentSnapshot, setOf(data)) as KaptClasspathChanges.Known
        assertEquals(emptySet<String>(), diff.names)
    }

    @Test
    fun testIncompatibleClasspaths() {
        val firstSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(File(""), listOf(File("a.jar")), listOf(), emptySet())
        val secondSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(File(""), listOf(File("b.jar")), listOf(), setOf(File("")))
        assertEquals(KaptClasspathChanges.Unknown, firstSnapshot.diff(secondSnapshot, emptySet()))
    }

    @Test
    fun testIncompatibleAnnotationProcessorClasspaths() {
        val firstSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(File(""), listOf(File("a.jar")), listOf(File("ap2.jar")), emptySet())
        val secondSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(
                File(""),
                listOf(File("a.jar")),
                listOf(File("ap1.jar")),
                setOf(File(""))
            )
        assertEquals(KaptClasspathChanges.Unknown, firstSnapshot.diff(secondSnapshot, emptySet()))
    }

    @Test
    fun testChangedClassesFound() {
        val dataFile = generateStructureData(
            ClassData("first/A"),
            ClassData("first/B").also { it.withAbiDependencies("first/A") }
        )
        val firstSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(tmp.newFolder(), listOf(), listOf(), setOf(dataFile))
        firstSnapshot.writeToCache()

        generateStructureData(
            ClassData("first/A", ByteArray(1)),
            ClassData("first/B").also { it.withAbiDependencies("first/A") },
            outputFile = dataFile
        )
        val changedSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(tmp.newFolder(), listOf(), listOf(), setOf(dataFile))

        val diff = changedSnapshot.diff(firstSnapshot, setOf(dataFile)) as KaptClasspathChanges.Known
        assertEquals(setOf("first/A", "first/B"), diff.names)
    }

    @Test
    fun testChangedClassesAcrossEntries() {
        val dataFile = generateStructureData(
            ClassData("first/A").also { it.withAbiDependencies("library/C") },
            ClassData("first/B").also { it.withAbiDependencies("first/A") }
        )

        val libraryDataFile = generateStructureData(ClassData("library/C"))

        val cacheDir = tmp.newFolder()
        val firstSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(cacheDir, listOf(), listOf(), setOf(dataFile, libraryDataFile))
        firstSnapshot.writeToCache()

        generateStructureData(ClassData("library/C", ByteArray(1)), outputFile = libraryDataFile)
        val changedSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(cacheDir, listOf(), listOf(), setOf(dataFile, libraryDataFile))

        val diff = changedSnapshot.diff(firstSnapshot, setOf(libraryDataFile)) as KaptClasspathChanges.Known
        assertEquals(setOf("library/C", "first/A", "first/B"), diff.names)
    }

    @Test
    fun testNoChangedFileButPathsChanged() {
        val dataFile = generateStructureData(
            ClassData("first/A"),
            ClassData("first/B").also { it.withAbiDependencies("first/A") }
        )
        val firstSnapshot = ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(tmp.newFolder(), listOf(), listOf(), setOf(dataFile))
        firstSnapshot.writeToCache()

        val copyOfDataFile = dataFile.copyTo(tmp.newFile(), overwrite = true)
        val secondSnapshot =
            ClasspathSnapshot.ClasspathSnapshotFactory.createCurrent(tmp.newFolder(), listOf(), listOf(), setOf(copyOfDataFile))

        val diff = secondSnapshot.diff(firstSnapshot, setOf()) as KaptClasspathChanges.Known
        assertEquals(emptySet<String>(), diff.names)
    }

    private fun generateStructureData(vararg classData: ClassData, outputFile: File = tmp.newFile()): File {
        val data = ClasspathEntryData()
        classData.forEach {
            data.classAbiHash[it.internalName] = it.hash
            data.classDependencies[it.internalName] = ClassDependencies(it.abiDeps, it.privateDeps)
        }
        data.saveTo(outputFile)

        return outputFile
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