#!/usr/bin/env kotlin

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun getCommits(fromRevision: String, toRevision: String, path: String?): List<Commit> {
    val cmd = "git rev-list --format=medium --ancestry-path $fromRevision..$toRevision ${path ?: "."}"
    val process = ProcessBuilder(*(cmd.split(" ")).toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    process.waitFor(60, TimeUnit.MINUTES)

    val commits = process.inputStream.bufferedReader().readText().split("\n\ncommit ")
    return commits.mapNotNull { commit ->
        val commitId = matchCommit.find(commit)?.groupValues?.let { it[1] } ?: return@mapNotNull null
        val changeId = matchChangeId.find(commit)?.groupValues?.let { it[1] }
        val relnote = matchRelnote.find(commit)?.groupValues?.let { it[1] }
        val issues = matchIssue.findAll(commit).mapNotNull { it.groups[1]?.value }.toList()
        return@mapNotNull Commit(commitId, changeId, relnote, issues)
    }
}

val matchCommit = Regex("^([0-9a-f]+)\n", RegexOption.IGNORE_CASE)
val matchRelnote =
    Regex(
        "^\\s*Relnote:\\s+(\"{3}.+\"{3}|\".+\"|[^\\n]+)$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
    )
val matchChangeId = Regex("Change-Id:\\s+(I[0-9a-f]+)", RegexOption.IGNORE_CASE)
val matchIssue = Regex("(?:Bug|Fixes):\\s+(\\d+)", RegexOption.IGNORE_CASE)

data class Commit(
    val commit: String,
    val changeId: String?,
    val relnote: String?,
    val issues: List<String>,
)

fun commitToGitHubUrl(commit: String) = "https://github.com/JetBrains/kotlin/commit/$commit"
fun changeIdToGerritUrl(changeId: String) = "https://android-review.googlesource.com/q/$changeId"
fun issueToBuganizerUrl(issue: String): String = "https://issuetracker.google.com/issue/$issue"

fun Commit.asReleaseNote(): String {
    val commitLink = "[${commit.substring(0, 7)}](${commitToGitHubUrl(commit)})"
    val issueLinks = issues.map { issue -> "[b/$issue](${issueToBuganizerUrl(issue)})" }.joinToString(", ")
    return "$commitLink $relnote $issueLinks"
}

if (args.isEmpty()) {
    println(
        """
        Usage: <from-tag> <to-tag> [<path>] 
        
        For example, to generate release notes for v2.0.0-RC2:
          <script> v2.0.0-RC1 v2.0.0-RC2
        """.trimIndent()
    )
    exitProcess(1)
}

val ignoreRelnotes = listOf("n/a")

val fromRevision = args[0]
val toRevision = args[1]
val path = args.getOrNull(2)

getCommits(fromRevision, toRevision, path)
    .filterNot { it.relnote == null || ignoreRelnotes.contains(it.relnote.toLowerCase()) }
    .map { it.asReleaseNote() }
    .forEach { println(it) }
