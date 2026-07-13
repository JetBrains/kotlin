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

suspend fun runReview(
    gitTree: GitTree,
    base: GitRevision,
    agent: Agent,
): ReviewResult {
    val diff = gitTree.getDiffFromMergeBase(base)
    return runReview(gitTree.project, diff, agent)
}

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

    val agentResults = ruleToFiles.entries.map { [rule, changedFiles] ->
        async {
            semaphore.withPermit {
                agent.checkRule(rule, changedFiles, diff.origin)
            }
        }
    }.awaitAll()

    ReviewResult(agentResults, diff.origin)
}
