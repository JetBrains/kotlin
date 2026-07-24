/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ContentEqualsIgnoringLineEndingsTest {

    @Test
    fun `expect true for two empty files`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt")
        val file2 = tempDir.resolve("file2.txt")
        file1.createNewFile()
        file2.createNewFile()
        assertTrue(contentEqualsIgnoringLineEndings(file1, file2))
        assertTrue(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect true for identical single-line files without trailing newline`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("same")
        val file2 = tempDir.resolve("file2.txt").writeLines("same")
        assertTrue(contentEqualsIgnoringLineEndings(file1, file2))
        assertTrue(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect true for identical multi-line files`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("same", "line1", "line2")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "line1", "line2")
        assertTrue(contentEqualsIgnoringLineEndings(file1, file2))
        assertTrue(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect true for same content with LF and CRLF line endings`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("same", "line1", "line2")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "line1", "line2", ending = "\r\n")
        assertTrue(contentEqualsIgnoringLineEndings(file1, file2))
        assertTrue(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect true for same content with LF and CR line endings`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("same", "line1", "line2")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "line1", "line2", ending = "\r")
        assertTrue(contentEqualsIgnoringLineEndings(file1, file2))
        assertTrue(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when one file has an extra line`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("\n", "same", "line1", "line2")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "line1", "line2")

        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when first line differs`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("different", "line1", "line2")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "line1", "line2")
        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when middle line differs`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("same", "different", "line2")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "line1", "line2")
        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when last line differs`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("same", "line1", "different")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "line1", "line2")
        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when only character casing differs`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("same", "line1", "line2")
        val file2 = tempDir.resolve("file2.txt").writeLines("same", "LINE1", "line2")
        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when only whitespace differs`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("l1\t", "l2")
        val file2 = tempDir.resolve("file2.txt").writeLines("l1", "l2")
        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when one file has an extra newline in the middle`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("l1", "\n", "l2", "l3")
        val file2 = tempDir.resolve("file2.txt").writeLines("l1", "l2", "l3")
        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect true when both files contain matching empty lines`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("\n", "\n", "\n")
        val file2 = tempDir.resolve("file2.txt").writeLines("\n", "\n", "\n")
        assertTrue(contentEqualsIgnoringLineEndings(file1, file2))
        assertTrue(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect false when trailing blank lines differ`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("l1", "l2", "\n", "\n")
        val file2 = tempDir.resolve("file2.txt").writeLines("l1", "l2")
        assertFalse(contentEqualsIgnoringLineEndings(file1, file2))
        assertFalse(contentEqualsIgnoringLineEndings(file2, file1))
    }

    @Test
    fun `expect true when both files have matching trailing blank lines`(@TempDir tempDir: File) {
        val file1 = tempDir.resolve("file1.txt").writeLines("l1", "l2", "\n", "\n")
        val file2 = tempDir.resolve("file2.txt").writeLines("l1", "l2", "\n", "\n")
        assertTrue(contentEqualsIgnoringLineEndings(file1, file2))
        assertTrue(contentEqualsIgnoringLineEndings(file2, file1))
    }

    companion object {
        private fun File.writeLines(vararg lines: String, ending: String = "\n"): File {
            writeText(lines.joinToString(ending))
            return this
        }
    }
}
