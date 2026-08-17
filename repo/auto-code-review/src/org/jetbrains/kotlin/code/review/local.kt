/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import java.io.File

suspend fun main(args: Array<String>) {
    check(args.size <= 3) {
        "Too many arguments. Expected <output> <repoRoot> [<baseRev>]"
    }

    val output = File(args[0])
    val repoRoot = File(args[1])
    val baseRevString = args.getOrNull(2) ?: "origin/master"

    val gitTree = GitWorkingTree(repoRoot, GitCLI)
    val baseRev = GitRevision(baseRevString)
    val agent = LocalClaudeAgent.create(gitTree.project)

    val diff = gitTree.getDiffFromMergeBase(baseRev)
    val reviewResult = runReview(gitTree.project, diff, agent)

    writeLocalReport(output, gitTree.project, reviewResult)

    val outputUrl = output.toURI().toURL()

    reviewResult.firstException?.let { exception ->
        throw Exception(
            "Review (partially) failed. See more details in the generated report:\n$outputUrl",
            exception
        )
    }

    println("Review generated:")
    println(outputUrl)
}

private suspend fun GitTree.getDiffFromMergeBase(revision: GitRevision): GitDiff {
    val mergeBase = getMergeBase(revision)
    if (revision.rev == "HEAD" || revision.rev.startsWith("HEAD~") || revision.rev.startsWith("HEAD^")) {
        // The intention is clear, there is no room for mistake.
    } else {
        val numberOfCommits = countCommitsAfter(mergeBase)
        if (numberOfCommits > 100) {
            // Maybe the `revision` is the wrong branch. For example, it is `origin/master`,
            // but the tree is branched off from a release branch.
            error(
                """
                    Suspicious input: base=${revision.rev} effective base = ${mergeBase.sha1}.
                    It is $numberOfCommits commits. Is it not too many?
                    Run with base = HEAD~$numberOfCommits to indicate that it is intentional.
                """.trimIndent()
            )
        }
    }
    return getDiffFrom(mergeBase)
}

private fun writeLocalReport(output: File, project: LocalProject, reviewResult: ReviewResult) {
    val text = with(LocalRenderingContext(output, project)) {
        render(reviewResult)
    }
    output.writeText(text)
}

private class LocalRenderingContext(output: File, project: LocalProject) : RenderingContext {
    private val repoRootRelative = project.root.relativeTo(output.parentFile)

    private val ProjectFilePath.pathRelativeToOutput: String
        get() = repoRootRelative.resolve(this.pathFromProjectRoot).invariantSeparatorsPath

    override fun codeLink(path: ProjectFilePath, line: Int): String {
        val url = path.pathRelativeToOutput
        val fileName = path.fileName
        return "[$fileName]($url):$line"
    }

    override fun markdownLink(path: ProjectFilePath, title: String): String {
        val fileUrl = path.pathRelativeToOutput
        val anchor = slugifyMarkdownTitle(title)
        return "[$title]($fileUrl#$anchor)"
    }

    override fun describeDiff(origin: GitDiff.Origin): String {
        return when (origin) {
            is GitDiff.Origin.Local -> "`git diff ${origin.from.sha1}` at `${origin.to.root}`"
            is GitDiff.Origin.GitHub -> origin.compareMarkdownLink
        }
    }
}
