/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals

class UnifiedDiffTest {

    private fun diff(
        expected: List<String>,
        actual: List<String>,
        contextLines: Int = 3,
        maxOutputLines: Int = 200,
    ): String = unifiedDiff(expected, actual, "expected", "actual", contextLines, maxOutputLines)

    @Test
    fun identicalInputsProduceEmptyDiff() {
        assertEquals("", diff(listOf("a", "b"), listOf("a", "b")))
        assertEquals("", diff(emptyList(), emptyList()))
    }

    @Test
    fun singleChangedLine() {
        assertEquals(
            """
            --- expected
            +++ actual
            @@ -1,5 +1,5 @@
             a
             b
            -c
            +X
             d
             e
            """.trimIndent(),
            diff(listOf("a", "b", "c", "d", "e"), listOf("a", "b", "X", "d", "e")),
        )
    }

    @Test
    fun insertionIntoEmpty() {
        assertEquals(
            """
            --- expected
            +++ actual
            @@ -0,0 +1,2 @@
            +x
            +y
            """.trimIndent(),
            diff(emptyList(), listOf("x", "y")),
        )
    }

    @Test
    fun deletionAtEnd() {
        assertEquals(
            """
            --- expected
            +++ actual
            @@ -1,3 +1,2 @@
             a
             b
            -c
            """.trimIndent(),
            diff(listOf("a", "b", "c"), listOf("a", "b")),
        )
    }

    @Test
    fun distantChangesAreSplitIntoHunks() {
        val expected = (1..20).map { "l$it" }
        val actual = expected.toMutableList().apply {
            this[1] = "X"
            this[18] = "Y"
        }
        assertEquals(
            """
            --- expected
            +++ actual
            @@ -1,4 +1,4 @@
             l1
            -l2
            +X
             l3
             l4
            @@ -17,4 +17,4 @@
             l17
             l18
            -l19
            +Y
             l20
            """.trimIndent(),
            diff(expected, actual, contextLines = 2),
        )
    }

    @Test
    fun closeChangesAreMergedIntoOneHunk() {
        val expected = (1..10).map { "l$it" }
        val actual = expected.toMutableList().apply {
            this[1] = "X"
            this[7] = "Y"
        }
        assertEquals(
            """
            --- expected
            +++ actual
            @@ -1,10 +1,10 @@
             l1
            -l2
            +X
             l3
             l4
             l5
             l6
             l7
            -l8
            +Y
             l9
             l10
            """.trimIndent(),
            diff(expected, actual, contextLines = 3),
        )
    }

    @Test
    fun outputIsTruncated() {
        assertEquals(
            """
            --- expected
            +++ actual
            @@ -1,5 +1,5 @@
            ... (6 more lines)
            """.trimIndent(),
            diff(listOf("a", "b", "c", "d", "e"), listOf("a", "b", "X", "d", "e"), maxOutputLines = 3),
        )
    }

    @Test
    fun hugeDifferenceFallsBackToFullReplacement() {
        val expected = (1..1500).map { "a$it" }
        val actual = (1..1500).map { "b$it" }
        assertEquals(
            """
            --- expected
            +++ actual
            @@ -1,1500 +1,1500 @@
            -a1
            ... (2999 more lines)
            """.trimIndent(),
            diff(expected, actual, maxOutputLines = 4),
        )
    }

    @Test
    fun lineEndingsAreIgnoredForFiles(@TempDir tempDir: File) {
        val crlf = tempDir.resolve("crlf.lock").apply { writeText("a\r\nb\r\n") }
        val lf = tempDir.resolve("lf.lock").apply { writeText("a\nb\n") }
        assertEquals("", unifiedDiff(crlf, lf))
    }
}
