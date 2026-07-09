/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.serialization.Serializable

@Serializable
class RuleReviewResult(val comments: List<Comment>) {
    @Serializable
    class Comment(val relativeFilePath: ProjectFilePath, val line: Int, val message: String)
}

class ReviewMeta(val costUSD: Double)

sealed class AgentResult(val rule: CodeRule, val meta: ReviewMeta?) {
    val costUSD: Double? get() = meta?.costUSD

    class Success(
        rule: CodeRule,
        val reviewResult: RuleReviewResult,
        meta: ReviewMeta,
    ) : AgentResult(rule, meta)

    class Failure(
        rule: CodeRule,
        meta: ReviewMeta?,
        val exception: Throwable,
        val executionResult: ExecutionResult?,
        val output: String?
    ) : AgentResult(rule, meta)
}

class ReviewResult(val agentResults: List<AgentResult>, val diffOrigin: GitDiff.Origin) {
    val successfulAgentResults: List<AgentResult.Success> = agentResults.filterIsInstance<AgentResult.Success>()
    val failingAgentResults: List<AgentResult.Failure> = agentResults.filterIsInstance<AgentResult.Failure>()

    val firstException: Throwable? = failingAgentResults.firstOrNull()?.exception
}
