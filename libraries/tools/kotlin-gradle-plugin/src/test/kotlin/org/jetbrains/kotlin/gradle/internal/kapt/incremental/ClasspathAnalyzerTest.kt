/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.incremental

import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
            dir.resolve("ignore.txt").writeBytes(emptyClass("test/B"))
            dir.resolve("META-INF/versions/9/A.class").also {
                it.parentFile.mkdirs()
                it.writeBytes(emptyClass("A"))
            }
        }
        val transform = StructureArtifactTransform().also { it.outputDirectory = tmp.newFolder() }
        val outputs = transform.transform(classesDir)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.single())
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

                it.putNextEntry(ZipEntry("META-INF/versions/9/test/A.class"))
                it.write(emptyClass("test/A"))
                it.closeEntry()
            }
        }
        val transform = StructureArtifactTransform().also { it.outputDirectory = tmp.newFolder() }
        val outputs = transform.transform(inputJar)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.single())
        assertEquals(setOf("test/A", "test/B"), data.classAbiHash.keys)
        assertEquals(setOf("test/A", "test/B"), data.classDependencies.keys)
        assertEquals(emptySet<String>(), data.classDependencies["test/A"]!!.abiTypes)
        assertEquals(emptySet<String>(), data.classDependencies["test/A"]!!.privateTypes)

        assertEquals(emptySet<String>(), data.classDependencies["test/B"]!!.abiTypes)
        assertEquals(emptySet<String>(), data.classDependencies["test/B"]!!.privateTypes)
    }

    @Test
    fun emptyInput() {
        val inputDir = tmp.newFolder("input")
        val transform = StructureArtifactTransform().also { it.outputDirectory = tmp.newFolder() }
        val outputs = transform.transform(inputDir)

        val data = ClasspathEntryData.ClasspathEntrySerializer.loadFrom(outputs.single())
        assertTrue(data.classAbiHash.isEmpty())
        assertTrue(data.classDependencies.isEmpty())
    }

    private fun emptyClass(internalName: String): ByteArray {
        val writer = ClassWriter(Opcodes.API_VERSION)
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", emptyArray())
        return writer.toByteArray()
    }
}