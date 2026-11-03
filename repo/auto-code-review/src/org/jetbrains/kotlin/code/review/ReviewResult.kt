/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.serialization.Serializable

@Serializable
class ReviewComments(val comments: List<ReviewComment>)

@Serializable
class ReviewComment(val relativeFilePath: ProjectFilePath, val line: Int, val message: String)

class ReviewMeta(val costUSD: Double)

/**
 * The result of one agent run: either the reported review comments, or a failure.
 */
sealed class AgentResult {
    abstract val meta: ReviewMeta?

    class Success(
        val comments: ReviewComments,
        override val meta: ReviewMeta,
    ) : AgentResult()

    class Failure(
        override val meta: ReviewMeta?,
        val exception: Throwable,
        val executionResult: ExecutionResult?,
        val output: String?
    ) : AgentResult()
}

/**
 * The outcome of checking one rule: the comments and failures of the agent runs that checked it,
 * and their meta information.
 */
class RuleReviewResult(
    val rule: CodeRule,
    val comments: List<ReviewComment>,
    val failures: List<AgentResult.Failure>,
    /**
     * The meta information of the agent runs, or null if it is unknown for one of them.
     */
    val meta: ReviewMeta?,
) {
    val costUSD: Double?
        get() = meta?.costUSD
}

class ReviewResult(val ruleResults: List<RuleReviewResult>, val diffOrigin: GitDiff.Origin) {
    val firstException: Throwable? = ruleResults.firstNotNullOfOrNull { it.failures.firstOrNull()?.exception }
}
