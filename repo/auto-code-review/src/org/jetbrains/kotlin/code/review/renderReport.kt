/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

interface RenderingContext {
    fun codeLink(path: ProjectFilePath, line: Int): String
    fun markdownLink(path: ProjectFilePath, title: String): String

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
    get() = "[${base.rev}...${to.sha1}](https://github.com/${repository}/compare/${base.rev}...${to.rev})"

private const val WARNING_EMOJI = "⚠\uFE0F"

context(renderingContext: RenderingContext)
fun render(review: ReviewResult): String = buildString {
    appendOrigin(review)
    appendFailures(review)
    appendReview(review)
    appendMeta(review)
}

context(renderingContext: RenderingContext)
fun StringBuilder.appendOrigin(review: ReviewResult) {
    val readme = ProjectFilePath("repo/auto-code-review/README.md")
    appendLine(
        """
            ${renderingContext.markdownLink(readme, "Auto Code Review")} for
            ${renderingContext.describeDiff(review.diffOrigin)}
            
        """.trimIndent()
    )
}

context(renderingContext: RenderingContext)
private fun StringBuilder.appendFailures(review: ReviewResult) {
    val failures = review.failingAgentResults
    val failure = failures.firstOrNull() ?: return
    appendLine(
        """
            # $WARNING_EMOJI ${failures.size} failures happened
            First failure:
            
            Rule: ${renderingContext.ruleLink(failure.rule)}
            
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
    if (review.successfulAgentResults.all { it.reviewResult.comments.isEmpty() }) {
        appendLine("# No rule violations found")
    }

    for (result in review.successfulAgentResults) {
        if (result.reviewResult.comments.isEmpty()) continue
        appendLine("# ${renderingContext.ruleLink(result.rule)}")
        appendLine()
        result.reviewResult.comments.forEach {
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
}

context(renderingContext: RenderingContext)
private fun StringBuilder.appendMeta(review: ReviewResult) = appendCollapsed("Meta") {
    appendLine(
        """
            # Meta
            | Rule | Comments # | Cost, $ |
            | --- | --- | --- |
        """.trimIndent()
    )

    val costs = review.agentResults.map { it.costUSD }
    val totalComments = review.successfulAgentResults.sumOf { it.reviewResult.comments.size }

    appendLine(
        "| **Total** | %s%d | %s%.2f |".format(
            if (review.failingAgentResults.isNotEmpty()) WARNING_EMOJI else "",
            totalComments,
            if (costs.any { it == null }) WARNING_EMOJI else "",
            costs.sumOf { it ?: 0.0 }
        )
    )
    review.agentResults.sortedByDescending { it.costUSD }.forEach { result ->
        val rule = result.rule
        val link = renderingContext.ruleLink(rule)

        val comments: String = when (result) {
            is AgentResult.Failure -> WARNING_EMOJI
            is AgentResult.Success -> {
                val count = result.reviewResult.comments.size
                val countString = count.toString()
                if (count == 0) {
                    countString
                } else {
                    renderingContext.localLink(countString, rule.name) ?: countString
                }
            }
        }

        val cost = if (result.costUSD != null) {
            "%.2f".format(result.costUSD)
        } else {
            WARNING_EMOJI
        }

        appendLine("| $link | $comments | $cost |")
    }
}

fun slugifyMarkdownTitle(title: String): String =
    title
        .replace(" ", "-")
        .filter { it.isLetterOrDigit() || it == '-' }
        .replace(minusesRegex, "-")
        .lowercase()

private val minusesRegex = Regex("-+")
