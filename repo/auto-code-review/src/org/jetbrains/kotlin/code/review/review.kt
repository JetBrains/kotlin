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
            val batchResults = splitIntoBatches(changedFiles).map { batch ->
                async {
                    semaphore.withPermit {
                        agent.checkRule(rule, batch, diff.origin)
                    }
                }
            }.awaitAll()

            mergeBatchResults(rule, batchResults)
        }
    }.awaitAll()

    ReviewResult(ruleResults, diff.origin, agent.info)
}

/**
 * The maximum size of the diff of one batch of files, in characters.
 *
 * The diff of a batch is what a single agent run receives, so this bounds how much the agent
 * has to process at once. The value leaves enough room in the agent's context window for it
 * to also read the files from the repository.
 *
 * The arithmetic: a token represents about 3.5 English characters
 * (https://platform.claude.com/docs/en/about-claude/glossary#tokens),
 * so 256K characters is roughly 75K tokens — well under even a 200K-token context window.
 */
internal const val MAX_DIFF_CHARS_PER_BATCH = 256 * 1024

/**
 * Splits the changed files into batches, so that the diff of each batch
 * is at most [MAX_DIFF_CHARS_PER_BATCH] characters
 * (except when a single file's diff alone exceeds the limit),
 * and a single agent run doesn't have to process more than fits in its context.
 *
 * A single file is never split between batches: its diff lines are meaningless without the file
 * header. A file whose diff alone exceeds the limit forms a batch on its own.
 *
 * Note: we could have delegated the batching to the agent itself, by asking it to spawn subagents if necessary.
 * Unfortunately, Claude Code doesn't seem to support subagents when run in --bare mode,
 * which is used in [LocalClaudeAgent].
 */
internal fun splitIntoBatches(changedFiles: List<GitDiff.ChangedFile>): List<List<GitDiff.ChangedFile>> {
    val batches = ArrayList<List<GitDiff.ChangedFile>>()
    var batch = ArrayList<GitDiff.ChangedFile>()
    var batchDiffChars = 0

    for (changedFile in changedFiles) {
        val diffChars = changedFile.diffChars
        if (batch.isNotEmpty() && batchDiffChars + diffChars > MAX_DIFF_CHARS_PER_BATCH) {
            batches.add(batch)
            batch = ArrayList()
            batchDiffChars = 0
        }
        batch.add(changedFile)
        batchDiffChars += diffChars
    }

    if (batch.isNotEmpty()) batches.add(batch)

    return batches
}

internal val GitDiff.ChangedFile.diffChars: Int
    get() = patchLines.sumOf { it.length + 1 }

/**
 * Merges the results of the agent runs that checked one rule's files:
 * comments and failures are combined, and so is the meta information.
 */
internal fun mergeBatchResults(rule: CodeRule, batchResults: List<AgentResult>): RuleReviewResult {
    val comments = batchResults.flatMap { result ->
        when (result) {
            is AgentResult.Success -> result.comments.comments
            is AgentResult.Failure -> emptyList()
        }
    }

    return RuleReviewResult(
        rule = rule,
        comments = comments,
        failures = batchResults.filterIsInstance<AgentResult.Failure>(),
        meta = mergeMetas(batchResults.map { it.meta }),
    )
}

/**
 * Combines the meta information of the agent runs,
 * or returns null if it is unknown for one of them.
 */
private fun mergeMetas(metas: List<ReviewMeta?>): ReviewMeta? =
    ReviewMeta(metas.sumOf { it?.costUSD ?: return null })
