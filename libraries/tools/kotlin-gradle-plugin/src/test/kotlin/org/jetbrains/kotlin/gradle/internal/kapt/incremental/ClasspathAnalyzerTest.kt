/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.gradle.api.artifacts.transform.TransformOutputs
import org.jetbrains.kotlin.gradle.testing.WithTemporaryFolder
import org.jetbrains.kotlin.gradle.testing.newTempDirectory
import org.jetbrains.kotlin.gradle.testing.newTempFile
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClasspathAnalyzerTest : WithTemporaryFolder {
    @field:TempDir
    override lateinit var temporaryFolder: Path


    @Test
    fun testDirectory() {
        val classesDir = newTempDirectory().also { dir ->
            Files.createDirectories(dir.resolve("test"))
            Files.write(dir.resolve("test/A.class"), emptyClass("test/A"))
            Files.write(dir.resolve("test/B.class"), emptyClass("test/B"))
            Files.write(dir.resolve("ignore.txt"), byteArrayOf())
            Files.write(dir.resolve("module-info.class"), byteArrayOf())
            Files.createDirectories(dir.resolve("META-INF/versions/9"))
            Files.write(dir.resolve("META-INF/versions/9/A.class"), emptyClass("A"))
        }
        val outputs = TransformOutputsMock(newTempDirectory().toFile())
        transform(classesDir, outputs)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.createdOutputs.single())
        assertEquals(setOf("test/A", "test/B"), data.classAbiHash.keys)
        assertEquals(setOf("test/A", "test/B"), data.classDependencies.keys)
        assertEquals(emptySet<String>(), data.classDependencies["test/A"]!!.abiTypes)
        assertEquals(emptySet<String>(), data.classDependencies["test/A"]!!.privateTypes)

        assertEquals(emptySet<String>(), data.classDependencies["test/B"]!!.abiTypes)
        assertEquals(emptySet<String>(), data.classDependencies["test/B"]!!.privateTypes)
    }

    @Test
    fun testJar() {
        val inputJar = newTempFile("input.jar").also { jar ->
            ZipOutputStream(Files.newOutputStream(jar)).use {
                it.putNextEntry(ZipEntry("test/A.class"))
                it.write(emptyClass("test/A"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("test/B.class"))
                it.write(emptyClass("test/B"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("ignored.txt"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("module-info.class"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("META-INF/versions/9/test/A.class"))
                it.write(emptyClass("test/A"))
                it.closeEntry()
            }
        }
        val outputs = TransformOutputsMock(newTempDirectory().toFile())
        transform(inputJar, outputs)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.createdOutputs.single())
        assertEquals(setOf("test/A", "test/B"), data.classAbiHash.keys)
        assertEquals(setOf("test/A", "test/B"), data.classDependencies.keys)
        assertEquals(emptySet<String>(), data.classDependencies["test/A"]!!.abiTypes)
        assertEquals(emptySet<String>(), data.classDependencies["test/A"]!!.privateTypes)

        assertEquals(emptySet<String>(), data.classDependencies["test/B"]!!.abiTypes)
        assertEquals(emptySet<String>(), data.classDependencies["test/B"]!!.privateTypes)
    }

    @Test
    fun testJarWithEntriesShuffled() {
        val jarA = newTempFile("inputA.jar").also { jar ->
            ZipOutputStream(Files.newOutputStream(jar)).use {
                it.putNextEntry(ZipEntry("test/A.class"))
                it.write(emptyClass("test/A"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("test/B.class"))
                it.write(emptyClass("test/B"))
                it.closeEntry()
            }
        }
        val outputsA = TransformOutputsMock(newTempDirectory().toFile())
        transform(jarA, outputsA)

        val jarB = newTempFile("inputB.jar").also { jar ->
            ZipOutputStream(Files.newOutputStream(jar)).use {
                it.putNextEntry(ZipEntry("test/B.class"))
                it.write(emptyClass("test/B"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("test/A.class"))
                it.write(emptyClass("test/A"))
                it.closeEntry()
            }
        }
        val outputsB = TransformOutputsMock(newTempDirectory().toFile())
        transform(jarB, outputsB)

        assertContentEquals(outputsA.createdOutputs.single().readBytes(), outputsB.createdOutputs.single().readBytes())
    }

    @Test
    fun emptyInput() {
        val outputs = TransformOutputsMock(newTempDirectory().toFile())
        transform(newTempDirectory("input"), outputs)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.createdOutputs.single())
        assertTrue(data.classAbiHash.isEmpty())
        assertTrue(data.classDependencies.isEmpty())
    }

    @Test
    fun testJarsWithDependenciesWithinClasses() {
        val inputJar = newTempFile("input.jar").also { jar ->
            ZipOutputStream(Files.newOutputStream(jar)).use {
                it.putNextEntry(ZipEntry("test/A.class"))
                it.write(emptyClass("test/A", "test/B"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("test/B.class"))
                it.write(emptyClass("test/B"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("test/C.class"))
                it.write(emptyClass("test/C"))
                it.closeEntry()
            }
        }
        val outputs = TransformOutputsMock(newTempDirectory().toFile())
        transform(inputJar, outputs)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.createdOutputs.single())
        assertEquals(setOf("test/A", "test/B", "test/C"), data.classAbiHash.keys)
        assertEquals(setOf("test/A", "test/B", "test/C"), data.classDependencies.keys)
    }

    private fun emptyClass(internalName: String, superClass: String = "java/lang/Object"): ByteArray {
        val writer = ClassWriter(Opcodes.API_VERSION)
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, superClass, emptyArray())
        return writer.toByteArray()
    }
}

class TransformOutputsMock(val outputDir: File) : TransformOutputs {
    val createdOutputs = mutableListOf<File>()

    override fun file(name: Any): File {
        val newFile = outputDir.resolve(name as String)
        createdOutputs.add(newFile)
        return newFile
    }

    override fun dir(name: Any): File {
        val newDir = outputDir.resolve(name as String)
        createdOutputs.add(newDir)
        return newDir
    }

}
