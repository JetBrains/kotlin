/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test

import junit.framework.TestCase
import org.jetbrains.kotlin.kapt3.base.KaptPaths
import org.junit.Test
import java.io.File
import java.nio.file.Files

class KaptPathsTest : TestCase() {
    @Test
    fun testSymbolicLinks() {
        val tempDir = Files.createTempDirectory("kapt-test").toFile()
        try {
            fun File.writeJavaClass() = apply {
                parentFile.mkdirs()
                writeText("public class $nameWithoutExtension {}")
            }

            val outputDir = File(tempDir, "stubs").apply { mkdir() }
            val otherDir = File(tempDir, "other").apply { mkdir() }
            val javaRootDir = File(tempDir, "java").apply { mkdir() }

            val simpleJava = File(tempDir, "Simple.java").writeJavaClass()
            val otherJava = File(otherDir, "Other.java").writeJavaClass()
            val notJava = File(otherDir, "NotJava.not").writeJavaClass()
            File(javaRootDir, "JavaRoot.java").writeJavaClass()

            val symlinkToOtherJava = Files.createSymbolicLink(File(tempDir, "Other.java").toPath(), otherJava.toPath()).toFile()
            val symlinkToNotJava = Files.createSymbolicLink(File(tempDir, "NotJava.java").toPath(), notJava.toPath()).toFile()
            val symlinkToJavaRootDir = Files.createSymbolicLink(File(tempDir, "java2").toPath(), javaRootDir.toPath()).toFile()

            val javaRoots = listOf(simpleJava, symlinkToOtherJava, symlinkToNotJava, symlinkToJavaRootDir, javaRootDir)

            val paths = KaptPaths(
                projectBaseDir = null,
                compileClasspath = emptyList(),
                annotationProcessingClasspath = emptyList(),
                javaSourceRoots = javaRoots,
                sourcesOutputDir = outputDir,
                classFilesOutputDir = outputDir,
                stubsOutputDir = outputDir,
                incrementalDataOutputDir = null
            )

            val javaSourceFiles = paths.collectJavaSourceFiles()

            fun assertContains(path: String) {
                val available by lazy { javaSourceFiles.joinToString { it.toRelativeString(tempDir) } }
                assertTrue("Can't find path $path\nAvailable: $available",
                           javaSourceFiles.any { it.toRelativeString(tempDir) == path })
            }

            assertEquals(4, javaSourceFiles.size)
            assertContains("Simple.java")
            assertContains("Other.java")
            assertContains("NotJava.java")
            assertContains("java/JavaRoot.java")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}