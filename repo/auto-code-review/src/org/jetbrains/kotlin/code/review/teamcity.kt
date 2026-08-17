/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import kotlinx.coroutines.future.asDeferred
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

suspend fun main(args: Array<String>) {
    check(args.size <= 3) {
        "Too many arguments. Expected <output> <repoRoot> <baseRev>"
    }

    val output = File(args[0])
    val repoRoot = File(args[1])
    val baseRevString = args[2]

    val gitTree = GitWorkingTree(repoRoot, GitCLI)
    val agent = LocalClaudeAgent.create(gitTree.project)

    val headSha1 = gitTree.findHead()
    val baseRev = GitRevision(baseRevString)

    // Could probably be inferred from TeamCity parameters or `git remote get-url`,
    // but this isn't worth the hassle.
    val gitHubRepository = "JetBrains/kotlin"

    /*
    TeamCity uses the shallow clone by default, so computing the diff locally is not possible.

    Using full clone instead is undesirable for performance reasons.

    Potential trade-off: set specific clone depth with `teamcity.git.agent.shallowCloneDepth`.
    But it is clumsy, as it would limit the number of commits between the base branch and HEAD;
    also, that approach would still require to fetch the base branch locally or find the merge base in
    another way.

    Instead, let's trivially fetch the diff from GitHub:
    */
    val diff = fetchDiffFromGitHub(gitHubRepository, baseRev, headSha1)

    val reviewResult = runReview(gitTree.project, diff, agent)

    val text = with(GitHubRenderingContext(gitHubRepository, headSha1)) {
        render(reviewResult)
    }
    output.writeText(text)

    reviewResult.firstException?.let { exception ->
        throw Exception(
            "Review (partially) failed. See more details in the generated report",
            exception
        )
    }
}

suspend fun fetchDiffFromGitHub(repository: String, base: GitRevision, to: GitSHA1): GitDiff {
    val origin = GitDiff.Origin.GitHub(repository, base, to)
    val text = fetchDiffTextFromGitHub(origin)
    return GitDiff(parseGitDiffText(text), origin)
}

private suspend fun fetchDiffTextFromGitHub(origin: GitDiff.Origin.GitHub): String {
    val rawDiffUrl = origin.rawDiffUrl

    val request = HttpRequest.newBuilder()
        .uri(URI.create(rawDiffUrl))
        .GET()
        .build()

    val response = HttpClient.newHttpClient().sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString()
    ).asDeferred().await()

    if (response.statusCode() !in 200..299) {
        throw Exception("Failed to fetch the diff from $rawDiffUrl: ${response.statusCode()} ${response.body()}")
    }

    return response.body()
}

private class GitHubRenderingContext(val repository: String, val sha1: GitSHA1) : RenderingContext {
    override fun codeLink(path: ProjectFilePath, line: Int): String {
        // Use plain=1 just in case it is a file with a custom rendering like Markdown.
        // Otherwise, a link to the line won't work.
        return "[${path.fileName}:$line](https://github.com/$repository/blob/${sha1.sha1}/$path?plain=1#L$line)"
    }

    override fun markdownLink(path: ProjectFilePath, title: String): String {
        return "[$title](https://github.com/$repository/blob/${sha1.sha1}/$path#${slugifyMarkdownTitle(title)})"
    }

    override fun localLink(text: String, title: String): String? {
        // When posting Markdown as a GitHub comment, it is tricky to have a link to a title in the same comment.
        // Let's keep it unsupported for now.
        return null
    }

    override fun describeDiff(origin: GitDiff.Origin): String = when (origin) {
        is GitDiff.Origin.Local ->
            GitDiff.Origin.GitHub(repository, origin.from, sha1).compareMarkdownLink
        is GitDiff.Origin.GitHub ->
            origin.compareMarkdownLink
    }
}
