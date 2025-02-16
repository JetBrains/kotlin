/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.lang.System
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun getCommits(fromRevision: String, toRevision: String): List<Commit> {
    val cmd = "git rev-list --format=medium $fromRevision..$toRevision ."
    val process = ProcessBuilder(*(cmd.split(" ")).toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
    process.waitFor(60, TimeUnit.MINUTES)

    val commits = process.inputStream.bufferedReader().readText().split("\n\ncommit ")
    return commits.mapNotNull { commit ->
        val sanitizedCommit = commit.removePrefix("commit ").padEnd(2, '\n')
        val commitGroups = parseCommit.find(sanitizedCommit)?.groupValues ?: return@mapNotNull null
        if (commitGroups.size != 4) {
            // group 1: commit hash
            // group 2: commit message
            // group 3: title
            return@mapNotNull null
        }
        val commitId = commitGroups[1]
        val title = commitGroups[3]
        val commitMessage = commitGroups[2]
        val changeId = matchChangeId.find(commitMessage)?.groupValues?.let { it[1] }
        val relnote = matchRelnote.find(commitMessage)?.groupValues?.let { it[1] }
        val issues = matchIssue.findAll(commitMessage).mapNotNull { it.groups[1]?.value }.toList()
        return@mapNotNull Commit(commitId, title, changeId, relnote?.trim('\"'), issues)
    }
}

val parseCommit =
    Regex("^([0-9a-f]+)\\nAuthor:.*?\\nDate:.*?\\n[\\s]+((.*?)\\n[\\s\\S]+)", RegexOption.MULTILINE)
val matchRelnote =
    Regex(
        "^\\s*Relnote:\\s+(\"{3}.+\"{3}|\".+\"|[^\\n]+)$",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
    )
val matchChangeId = Regex("Change-Id:\\s+(I[0-9a-f]+)", RegexOption.IGNORE_CASE)
val matchIssue = Regex("(?:Bug|Fixes):\\s+\\[?(\\d+)\\]?", RegexOption.IGNORE_CASE)

data class Commit(
    val commit: String,
    val title: String,
    val changeId: String?,
    val relnote: String?,
    val issues: List<String>,
)

fun commitToGitHubUrl(commit: String) = "https://github.com/JetBrains/kotlin/commit/$commit"
fun issueToBuganizerUrl(issue: String): String = "https://issuetracker.google.com/issues/$issue"

fun Commit.asReleaseNote(): String {
    val commitLink = "[`${commit.take(7)}`](${commitToGitHubUrl(commit)})"
    val issueLinks = issues.map { issue -> "[`b/$issue`](${issueToBuganizerUrl(issue)})" }.joinToString(", ")
    val link = if (issueLinks.isEmpty()) commitLink else issueLinks
    val text = (relnote ?: title).split(" ").joinToString(" ") { if (it.startsWith('@')) "`$it`" else it }
    return "- $link $text"
}

fun main(vararg args: String) {
    if (args.isEmpty()) {
        println(
            """
        Usage: ./gradlew composeReleaseNotes --args="<from-tag> <to-tag>:"
        
        For example, to generate release notes for v2.0.0-RC2:
          ./gradlew composeReleaseNotes --args="v2.0.0-RC1 v2.0.0-RC2"
        """.trimIndent()
        )
        exitProcess(1)
    }

    val ignoreRelnotes = listOf("n/a")

    val fromRevision = args[0]
    val toRevision = args[1]

    val (fixes, features) = getCommits(fromRevision, toRevision)
        .filter {
            (it.relnote != null && !ignoreRelnotes.contains(it.relnote.lowercase())) ||
                    it.issues.isNotEmpty()
        }
        .partition { it.issues.isNotEmpty() }

    println("### Compose compiler")
    println("#### New features")
    features.forEach { println(it.asReleaseNote()) }
    println("#### Fixes")
    fixes.forEach { println(it.asReleaseNote()) }
}