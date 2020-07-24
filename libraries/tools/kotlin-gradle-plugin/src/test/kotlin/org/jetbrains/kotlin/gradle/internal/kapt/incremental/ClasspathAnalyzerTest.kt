/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.internal.file.DefaultFileSystemLocation
import org.gradle.api.internal.provider.DefaultProvider
import org.gradle.api.provider.Provider
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Callable
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ClasspathAnalyzerTest {
    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    @Test
    fun testDirectory() {
        val classesDir = tmp.newFolder().also { dir ->
            dir.resolve("test").mkdirs()
            dir.resolve("test/A.class").writeBytes(emptyClass("test/A"))
            dir.resolve("test/B.class").writeBytes(emptyClass("test/B"))
            dir.resolve("ignore.txt").writeText("")
            dir.resolve("module-info.class").writeText("")
            dir.resolve("META-INF/versions/9/A.class").also {
                it.parentFile.mkdirs()
                it.writeBytes(emptyClass("A"))
            }
        }
        val outputs = TransformOutputsMock(tmp.newFolder())
        StructureTransformTestAction(classesDir).transform(outputs)

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
        val inputJar = tmp.newFile("input.jar").also { jar ->
            ZipOutputStream(jar.outputStream()).use {
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
        val outputs = TransformOutputsMock(tmp.newFolder())
        StructureTransformTestAction(inputJar).transform(outputs)

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
        val jarA = tmp.newFile("inputA.jar").also { jar ->
            ZipOutputStream(jar.outputStream()).use {
                it.putNextEntry(ZipEntry("test/A.class"))
                it.write(emptyClass("test/A"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("test/B.class"))
                it.write(emptyClass("test/B"))
                it.closeEntry()
            }
        }
        val outputsA = TransformOutputsMock(tmp.newFolder())
        StructureTransformTestAction(jarA).transform(outputsA)

        val jarB = tmp.newFile("inputB.jar").also { jar ->
            ZipOutputStream(jar.outputStream()).use {
                it.putNextEntry(ZipEntry("test/B.class"))
                it.write(emptyClass("test/B"))
                it.closeEntry()

                it.putNextEntry(ZipEntry("test/A.class"))
                it.write(emptyClass("test/A"))
                it.closeEntry()
            }
        }
        val outputsB = TransformOutputsMock(tmp.newFolder())
        StructureTransformTestAction(jarB).transform(outputsB)

        assertArrayEquals(outputsA.createdOutputs.single().readBytes(), outputsB.createdOutputs.single().readBytes())
    }

    @Test
    fun emptyInput() {
        val transformAction = StructureTransformTestAction(tmp.newFolder("input"))
        val outputs = TransformOutputsMock(tmp.newFolder())

        transformAction.transform(outputs)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.createdOutputs.single())
        assertTrue(data.classAbiHash.isEmpty())
        assertTrue(data.classDependencies.isEmpty())
    }

    @Test
    fun testJarsWithDependenciesWithinClasses() {
        val inputJar = tmp.newFile("input.jar").also { jar ->
            ZipOutputStream(jar.outputStream()).use {
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
        val transformAction = StructureTransformTestAction(inputJar)
        val outputs = TransformOutputsMock(tmp.newFolder())

        transformAction.transform(outputs)

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

class StructureTransformTestAction(val input: File) : StructureTransformAction() {
    override val inputArtifact: File = input

    override fun getParameters(): TransformParameters.None? {
        //no need for StructureTransformAction and so for test
        return null
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