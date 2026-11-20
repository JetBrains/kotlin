/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MergeBatchResultsTests {
    private val rule = CodeRule(
        name = "Rule",
        text = "",
        patterns = CodeRulePatterns(listOf("*")),
        source = ProjectFilePath("code-rules.md"),
    )

    private fun comment(path: String, line: Int) =
        ReviewComment(ProjectFilePath(path), line, "message")

    private fun success(vararg comments: ReviewComment, costUSD: Double): AgentResult.Success =
        AgentResult.Success(ReviewComments(comments.toList()), ReviewMeta(costUSD))

    private fun failure(exception: Exception, costUSD: Double?): AgentResult.Failure =
        AgentResult.Failure(
            meta = costUSD?.let { ReviewMeta(it) },
            exception = exception,
            executionResult = null,
            output = null,
        )

    @Test
    fun `successful batches are merged into one result`() {
        val firstComment = comment("a.kt", 1)
        val secondComment = comment("b.kt", 2)
        val merged = mergeBatchResults(
            rule,
            listOf(
                success(firstComment, costUSD = 1.0),
                success(secondComment, costUSD = 2.0),
            ),
        )

        // ReviewComment doesn't implement equals, so the same instances are expected.
        assertEquals(listOf(firstComment, secondComment), merged.comments)
        assertTrue(merged.failures.isEmpty())
        assertEquals(3.0, merged.costUSD)
    }

    @Test
    fun `failed batches are merged into one result`() {
        val firstException = RuntimeException("First")
        val merged = mergeBatchResults(
            rule,
            listOf(
                failure(firstException, costUSD = 1.0),
                failure(RuntimeException("Second"), costUSD = 2.0),
            ),
        )

        assertEquals(2, merged.failures.size)
        assertEquals(firstException, merged.failures.first().exception)
        assertEquals(3.0, merged.costUSD)
    }

    @Test
    fun `unknown cost of a failed batch makes the merged cost unknown`() {
        val merged = mergeBatchResults(
            rule,
            listOf(
                failure(RuntimeException("First"), costUSD = 1.0),
                failure(RuntimeException("Second"), costUSD = null),
            ),
        )

        assertNull(merged.costUSD)
    }

    @Test
    fun `violations and failure are reported together`() {
        val firstComment = comment("a.kt", 1)
        val secondComment = comment("b.kt", 2)
        val exception = RuntimeException("Failure")
        val merged = mergeBatchResults(
            rule,
            listOf(
                success(firstComment, costUSD = 1.0),
                failure(exception, costUSD = 2.0),
                success(secondComment, costUSD = 3.0),
            ),
        )

        assertEquals(listOf(firstComment, secondComment), merged.comments)
        assertEquals(listOf(exception), merged.failures.map { it.exception })
        // The cost is the total over all batches, failed ones included.
        assertEquals(6.0, merged.costUSD)
    }
}
