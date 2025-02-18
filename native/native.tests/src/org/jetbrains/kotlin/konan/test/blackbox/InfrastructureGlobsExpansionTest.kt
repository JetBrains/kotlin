/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.util.expandGlob
import org.jetbrains.kotlin.konan.test.blackbox.support.util.sanitizedName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import kotlin.io.path.createTempDirectory

@Tag("infrastructure")
class InfrastructureGlobsExpansionTest {
    private lateinit var testDir: File

    @BeforeEach
    fun setUp() {
        testDir = createTempDirectory(InfrastructureGlobsExpansionTest::class.java.sanitizedName).toFile()
    }

    @Test
    fun noGlobs() {
        val fileName = "file.kt"
        val dirNames = listOf(null, "foo", "foo/bar", "foo/bar/baz")

        dirNames.forEach { dirName ->
            val fullPattern = testDir.resolveNullable(dirName).resolve(fileName)
            val expansionResult = expandGlob(fullPattern)

            assertEquals(1, expansionResult.size)
            assertEquals(fullPattern, expansionResult.first())
        }
    }

    @Test
    fun filePattern() {
        val fileNames = listOf("one.kt", "two.kt", "three.kt", "four.java", "five.py")
        val dirNames = listOf(null, "foo", "foo/bar", "foo/bar/baz")
        val pattern = "*.kt"

        dirNames.forEach { dirName ->
            val dir = createDirWithFiles(dirName, fileNames)

            val fullPattern = dir.resolve(pattern)
            val expansionResult = expandGlob(fullPattern)

            val kotlinOnyFiles = findAllKotlinFiles(dirName)

            assertEquals(kotlinOnyFiles.size, expansionResult.size)
            assertEquals(kotlinOnyFiles, expansionResult.toSet())
        }
    }

    @Test
    fun dirPattern() {
        val fileNames = listOf("one.kt", "two.kt", "three.kt", "four.java", "five.py")
        val dirNames = listOf(null, "foo", "bar", "baz")
        val pattern = "ba*/*.kt" // covers "bar" & "baz" dirs

        dirNames.forEach { dirName -> createDirWithFiles(dirName, fileNames) }

        val fullPattern = testDir.resolve(pattern)
        val expansionResult = expandGlob(fullPattern)

        val kotlinOnyFiles = findAllKotlinFiles("bar", "baz")
        assertEquals(kotlinOnyFiles.size, expansionResult.size)
        assertEquals(kotlinOnyFiles, expansionResult.toSet())
    }

    @Test
    fun doubleStarPattern() {
        val fileNames = listOf("one.kt", "two.kt", "three.kt", "four.java", "five.py")
        val dirNames = listOf(null, "foo", "foo/bar", "foo/bar/baz")
        val pattern = "foo/**.kt" // covers "foo" and all subdirectories

        dirNames.forEach { dirName -> createDirWithFiles(dirName, fileNames) }

        val fullPattern = testDir.resolve(pattern)
        val expansionResult = expandGlob(fullPattern)

        val kotlinOnyFiles = findAllKotlinFiles("foo", "foo/bar", "foo/bar/baz")
        assertEquals(kotlinOnyFiles.size, expansionResult.size)
        assertEquals(kotlinOnyFiles, expansionResult.toSet())
    }

    private fun createDirWithFiles(dirName: String?, fileNames: Collection<String>): File {
        val dir = testDir.resolveNullable(dirName)
        dir.mkdirs()

        fileNames.forEach { fileName ->
            dir.resolve(fileName).apply { createNewFile() }
        }

        return dir
    }

    private fun findAllKotlinFiles(vararg dirNames: String?): Set<File> = buildSet {
        dirNames.forEach { dirName ->
            val dir = testDir.resolveNullable(dirName)
            assertTrue(dir.isDirectory) { "Directory does not exist or is not a directory: $dir ($dirName)." }

            val files = dir.listFiles()?.takeIf { it.isNotEmpty() }
                ?: fail { "Unexpectedly empty directory: $dir ($dirName)." }

            files.mapNotNullTo(this) { if (it.isFile && it.extension == "kt") it else null }
        }
    }

    companion object {
        private fun File.resolveNullable(nullable: String?): File = if (nullable != null) resolve(nullable) else this
    }
}
