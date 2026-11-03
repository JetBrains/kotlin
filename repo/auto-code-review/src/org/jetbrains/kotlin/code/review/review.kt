/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private suspend fun CodeRuleRepository.getRules(file: GitDiff.ChangedFile): Set<CodeRule> =
    file.oldPath?.let { getRules(it) }.orEmpty() +
            file.newPath?.let { getRules(it) }.orEmpty()

suspend fun runReview(
    project: Project,
    diff: GitDiff,
    agent: Agent,
): ReviewResult {
    val ruleRepo = CodeRuleRepository(project)

    val ruleToFiles = diff.changedFiles.flatMap { changedFile ->
        ruleRepo.getRules(changedFile).map { rule -> rule to changedFile }
    }.groupBy({ it.first }, { it.second })

    return runReview(ruleToFiles, diff, agent)
}

/**
 * Check each rule in parallel.
 */
private suspend fun runReview(
    ruleToFiles: Map<CodeRule, List<GitDiff.ChangedFile>>,
    diff: GitDiff,
    agent: Agent,
): ReviewResult = withContext(Dispatchers.Default) {
    // Limit the number of parallel agent runs.
    // Even if the remote API endpoint doesn't have any limits, local machine resources do.
    val semaphore = Semaphore(8)

    val ruleResults = ruleToFiles.entries.map { [rule, changedFiles] ->
        async {
            semaphore.withPermit {
                agent.checkRule(rule, changedFiles, diff.origin)
            }.toRuleReviewResult(rule)
        }
    }.awaitAll()

    ReviewResult(ruleResults, diff.origin)
}

/**
 * Converts the result of one agent run to the outcome of checking [rule].
 */
private fun AgentResult.toRuleReviewResult(rule: CodeRule): RuleReviewResult = when (this) {
    is AgentResult.Success -> RuleReviewResult(
        rule = rule,
        comments = comments.comments,
        failures = emptyList(),
        meta = meta,
    )
    is AgentResult.Failure -> RuleReviewResult(
        rule = rule,
        comments = emptyList(),
        failures = listOf(this),
        meta = meta,
    )
}
