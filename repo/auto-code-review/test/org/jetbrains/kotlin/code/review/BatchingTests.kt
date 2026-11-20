/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertEquals

class BatchingTests {
    /**
     * A changed file whose diff is exactly [diffChars] characters:
     * one patch line, each character of which counts as one, plus the line break.
     */
    private fun changedFile(path: String, diffChars: Int): GitDiff.ChangedFile {
        require(diffChars >= 1)
        val changedFile = GitDiff.ChangedFile(
            oldPath = ProjectFilePath(path),
            newPath = ProjectFilePath(path),
            patchLines = listOf("x".repeat(diffChars - 1)),
        )
        check(changedFile.diffChars == diffChars)
        return changedFile
    }

    @Test
    fun `small diff is a single batch`() {
        val files = listOf(
            changedFile("a.kt", MAX_DIFF_CHARS_PER_BATCH / 3),
            changedFile("b.kt", MAX_DIFF_CHARS_PER_BATCH / 3),
        )

        assertEquals(listOf(files), splitIntoBatches(files))
    }

    @Test
    fun `diff exceeding the batch size limit is split into batches`() {
        val files = listOf(
            changedFile("a.kt", MAX_DIFF_CHARS_PER_BATCH / 2),
            changedFile("b.kt", MAX_DIFF_CHARS_PER_BATCH / 2),
            changedFile("c.kt", MAX_DIFF_CHARS_PER_BATCH / 2),
        )

        // The diff of a and b together is exactly of the maximum size, so they share a batch.
        assertEquals(listOf(files.take(2), files.drop(2)), splitIntoBatches(files))
    }

    @Test
    fun `single file larger than the batch size limit forms a batch on its own`() {
        val files = listOf(
            changedFile("a.kt", MAX_DIFF_CHARS_PER_BATCH * 2),
            changedFile("b.kt", 1),
            changedFile("c.kt", 1),
        )

        assertEquals(listOf(files.take(1), files.drop(1)), splitIntoBatches(files))
    }

    @Test
    fun `empty diff has no batches`() {
        assertEquals(emptyList(), splitIntoBatches(emptyList()))
    }
}
