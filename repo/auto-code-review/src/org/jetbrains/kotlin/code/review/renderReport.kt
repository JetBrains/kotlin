/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

interface RenderingContext {
    fun codeLink(path: ProjectFilePath, line: Int): String
    fun markdownLink(path: ProjectFilePath, heading: String, linkText: String = heading): String

    /**
     * Returns a Markdown link to [title] in the report, or `null` if unsupported.
     */
    fun localLink(text: String, title: String): String? {
        val anchor = slugifyMarkdownTitle(title)
        return "[$text](#$anchor)"
    }

    fun describeDiff(origin: GitDiff.Origin): String

    fun ruleLink(rule: CodeRule): String = markdownLink(rule.source, rule.name)
}

val GitDiff.Origin.GitHub.compareMarkdownLink: String
    get() = "[${base.shortRev}...${to.shortSha1}](https://github.com/${repository}/compare/${base.rev}...${to.sha1})"

private const val WARNING_EMOJI = "⚠\uFE0F"
private const val INFO_EMOJI = "ℹ\uFE0F"

context(renderingContext: RenderingContext)
fun render(review: ReviewResult): String = buildString {
    appendOrigin(review)
    appendSummary(review)
    appendFailures(review)
    appendReview(review)
    appendFooter(review)
}

private val readme = ProjectFilePath("repo/auto-code-review/README.md")

context(renderingContext: RenderingContext)
fun StringBuilder.appendOrigin(review: ReviewResult) {
    val readmeLink = renderingContext.markdownLink(readme, "Auto Code Review")
    val diffDescription = renderingContext.describeDiff(review.diffOrigin)
    appendLine("🤖$readmeLink for $diffDescription.")
    appendLine()
}

fun StringBuilder.appendSummary(review: ReviewResult) {
    val violations = review.ruleResults.sumOf { it.comments.size }

    val summary = when {
        review.ruleResults.isEmpty() -> "${INFO_EMOJI}Found no rules applicable to the changed files."
        violations == 0 -> {
            val prefix = if (review.ruleResults.any { it.failures.isNotEmpty() }) {
                INFO_EMOJI
            } else {
                "✅"
            }
            "${prefix}Found no violations of the checked rules."
        }
        else -> "Found $violations violations of the checked rules."
    }
    appendLine(summary)
    appendLine()
}

context(renderingContext: RenderingContext)
private fun StringBuilder.appendFailures(review: ReviewResult) {
    val failedResults = review.ruleResults.filter { it.failures.isNotEmpty() }
    val firstFailedResult = failedResults.firstOrNull() ?: return
    val failedRuns = failedResults.sumOf { it.failures.size }
    val failure = firstFailedResult.failures.first()
    appendLine(
        """
            # $WARNING_EMOJI $failedRuns failures happened
            First failure:
            
            Rule: ${renderingContext.ruleLink(firstFailedResult.rule)}
            
        """.trimIndent()
    )
    failure.output?.let {
        appendLine("Output:")
        appendCodeBlock("text", it)
        appendLine()
    }
    failure.executionResult?.let { executionResult ->
        appendLine("Exit code: ${executionResult.exitCode}")
        appendLine()
        appendLine("stdout:")
        appendCodeBlock("json", executionResult.stdout)
        appendLine()
        appendLine("stderr:")
        appendCodeBlock("text", executionResult.stderr)
        appendLine()
    }
    appendLine("Stacktrace:")
    appendCodeBlock("text", failure.exception.stackTraceToString())
    appendLine()
}

private fun StringBuilder.appendCodeBlock(language: String, text: String) {
    appendLine("```$language")
    appendLine(text)
    appendLine("```")
}

context(renderingContext: RenderingContext)
private fun StringBuilder.appendReview(review: ReviewResult) {
    for (result in review.ruleResults) {
        if (result.comments.isEmpty()) continue
        appendLine("# ${renderingContext.ruleLink(result.rule)}")
        appendLine()
        result.comments.forEach {
            val link = renderingContext.codeLink(it.relativeFilePath, it.line)
            appendLine("## $link")
            appendLine(it.message)
            appendLine()
        }
    }
}

private fun StringBuilder.appendCollapsed(summary: String, appendContent: StringBuilder.() -> Unit) {
    appendLine("<details><summary>$summary</summary>")
    appendLine()
    appendContent()
    appendLine()
    appendLine("</details>")
    appendLine()
}

context(renderingContext: RenderingContext)
private fun StringBuilder.appendMeta(review: ReviewResult) {
    val agentInfo = review.agentInfo
    appendLine(
        """
            Agent: ${agentInfo.name}, model: `${agentInfo.model}`, effort: `${agentInfo.effort}`.
            
            | Rule | Comments # | Cost, $ |
            | --- | --- | --- |
        """.trimIndent()
    )

    val costs = review.ruleResults.map { it.costUSD }
    val totalComments = review.ruleResults.sumOf { it.comments.size }

    appendLine(
        "| **Total** | %s%d | %s%.2f |".format(
            if (review.ruleResults.any { it.failures.isNotEmpty() }) WARNING_EMOJI else "",
            totalComments,
            if (costs.any { it == null }) WARNING_EMOJI else "",
            costs.sumOf { it ?: 0.0 }
        )
    )
    review.ruleResults.sortedByDescending { it.costUSD }.forEach { result ->
        val link = renderingContext.ruleLink(result.rule)

        val cost = if (result.costUSD != null) {
            "%.2f".format(result.costUSD)
        } else {
            WARNING_EMOJI
        }

        appendLine("| $link | ${commentsCell(result)} | $cost |")
    }
}

context(renderingContext: RenderingContext)
private fun commentsCell(result: RuleReviewResult): String {
    val count = result.comments.size
    if (count == 0) {
        return if (result.failures.isEmpty()) "0" else WARNING_EMOJI
    }

    val countString = count.toString()
    val linkedCount = renderingContext.localLink(countString, result.rule.name) ?: countString
    return if (result.failures.isEmpty()) linkedCount else "$linkedCount $WARNING_EMOJI"
}

context(renderingContext: RenderingContext)
private fun StringBuilder.appendFooter(review: ReviewResult) {
    if (review.ruleResults.all { it.comments.isEmpty() && it.failures.isEmpty() }) {
        // This is generated right after the summary. No separate section needed.
    } else {
        appendLine("# Review details")
        appendLine()
    }

    if (review.ruleResults.isNotEmpty()) {
        appendCollapsed("🔍Checked ${review.ruleResults.size} rules") {
            appendMeta(review)
        }
    }

    val ruleFilesLink = renderingContext.markdownLink(readme, "Rule files", linkText = "Add or update rules!")
    appendLine("💡Miss something? $ruleFilesLink")
    appendLine()
}

fun slugifyMarkdownTitle(title: String): String =
    title
        .replace(" ", "-")
        .filter { it.isLetterOrDigit() || it == '-' }
        .replace(minusesRegex, "-")
        .lowercase()

private val minusesRegex = Regex("-+")
