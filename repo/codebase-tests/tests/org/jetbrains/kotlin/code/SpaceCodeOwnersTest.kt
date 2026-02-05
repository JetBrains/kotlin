/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import org.eclipse.jgit.ignore.FastIgnoreRule
import org.eclipse.jgit.ignore.IgnoreNode
import java.io.File
import kotlin.test.assertIs

class SpaceCodeOwnersTest : TestCase() {
    private val ownersFile = File(System.getProperty("codeOwnersTest.spaceCodeOwnersFile"))
    private val owners = parseCodeOwners(ownersFile)

    fun testOwnerListNoDuplicates() {
        val duplicatedOwnerListEntries = owners.permittedOwners.groupBy { it.name }
            .filterValues { occurrences -> occurrences.size > 1 }
            .values

        if (duplicatedOwnerListEntries.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("Duplicated OWNER_LIST entries in $ownersFile:")
                    for (group in duplicatedOwnerListEntries) {
                        group.joinTo(this, separator = "\n", postfix = "\n---")
                    }
                }
            )
        }
    }

    fun testOwnersAreAddedByTeamsOrEmailAddress() {
        val invalidOwners = owners.permittedOwners
            .filterNot { it.name.first() == '"' && it.name.last() == '"' }
            .filterNot { it.name.contains('@') }

        if (invalidOwners.isEmpty()) return

        fail(
            buildString {
                appendLine("Owner(s) ${invalidOwners.joinToString { it.name }} do not meet the required criteria:")
                appendLine("1. Team name in quotations")
                appendLine("2. User email address")
            }
        )
    }

    fun testUserOwnersHaveGitHubUsername() {
        val userOwnersWithoutGitHubUsername = owners.userOwners
            .filter { it.githubUsername.isBlank() }

        if (userOwnersWithoutGitHubUsername.isEmpty()) return

        fail(
            buildString {
                appendLine("The following USER_OWNER entries are missing GitHub usernames:")
                for (entry in userOwnersWithoutGitHubUsername) {
                    appendLine("  $entry")
                }
            }
        )
    }

    fun testIndividualOwnersAreDefinedAsUserOwners() {
        val emailOwners = owners.permittedOwners
            .filter { it.name.contains('@') }
            .map { it.name }
            .toSet()

        val userOwnerEmails = owners.userOwners.map { it.email }.toSet()

        val missingUserOwners = emailOwners - userOwnerEmails

        if (missingUserOwners.isEmpty()) return

        fail(
            buildString {
                appendLine("The following email owners must be defined using USER_OWNER directive with their GitHub username:")
                for (email in missingUserOwners) {
                    appendLine("  $email")
                }
            }
        )
    }

    fun testAllOwnersInOwnerList() {
        val permittedOwnerNames = owners.permittedOwners.map { it.name }.toSet()
        val problems = mutableListOf<String>()
        for (pattern in owners.patterns) {
            if (pattern !is OwnershipPattern.Pattern) continue
            for (owner in pattern.owners) {
                if (owner !in permittedOwnerNames) {
                    problems += "Owner ${owner.quoteIfContainsSpaces()} not listed in $ownersFile, but used in $pattern"
                }
            }
        }
        if (problems.isNotEmpty()) {
            fail(problems.joinToString("\n"))
        }
    }

    fun testBranchRulesHaveValidOwners() {
        val permittedOwnerNames = owners.permittedOwners.map { it.name }.toSet()
        val problems = mutableListOf<String>()
        for (rule in owners.branchRules) {
            for (owner in rule.owners) {
                if (owner !in permittedOwnerNames) {
                    problems += "Owner ${owner.quoteIfContainsSpaces()} not listed in $ownersFile, but used in $rule"
                }
            }
        }
        if (problems.isNotEmpty()) {
            fail(problems.joinToString("\n"))
        }
    }

    fun testFallbackRuleMatchEverything() {
        val fallbackRule = owners.patterns.first()
        assertEquals("Fallback rule must be '*', while it is $fallbackRule", "*", fallbackRule.pattern)
        assertIs<OwnershipPattern.Pattern>(fallbackRule, "Fallback rule must not be UNKNOWN, but it is $fallbackRule")
    }

    fun testPatterns() {
        val checker = FileOwnershipChecker(
            owners,
            root = File(".")
        )
        checker.check()

        val problems = mutableListOf<String>()

        if (checker.unmatchedFilesTop.isNotEmpty()) {
            problems.add(
                "Found files without owner, please add it to $ownersFile:\n" +
                        checker.unmatchedFilesTop.joinToString("\n") { "    $it" }
            )
        }

        val unusedPatterns = checker.unusedMatchers()
        if (unusedPatterns.isNotEmpty()) {
            problems.add(
                "Found unused patterns in $ownersFile:\n" +
                        unusedPatterns.joinToString("\n") { "    ${it.item}" }
            )
        }

        if (problems.isNotEmpty()) {
            fail(problems.joinToString("\n"))
        }
    }

    private class FileOwnershipChecker(
        owners: CodeOwners,
        val root: File
    ) {
        val matchers =
            owners.patterns
                .map { ItemUse(it, FastIgnoreRule(it.pattern)) }
                .reversed()

        val fallbackMatcher = matchers.last()

        val fileMatchers = matchers.filterNot { (_, rule) -> rule.dirOnly() }

        val ignoreTracker = GitIgnoreTracker()

        val unmatchedFilesTop = mutableListOf<File>()

        data class ItemUse(val item: OwnershipPattern, val rule: FastIgnoreRule) {

            var used: Boolean = false

            override fun toString(): String {
                return "use($item) = $used"
            }
        }

        fun List<ItemUse>.findFirstMatching(path: String, isDirectory: Boolean, parentMatch: ItemUse?): ItemUse? {
            val parentMatchLine = parentMatch?.item?.line
            // Here, input list should be reversed, so that
            // lines are in reverse direction
            // We then run matcher till find more specific rule or break when parent matches already
            // Ex:
            // (line = 10, pattern = /some/file),
            // (line = 5, pattern = /some/),
            // (line = 1, pattern = *)
            // With input of parent = (line = 5, pattern = /some/) and path = /some/other
            // we only search till our parent pattern line, as other rules are less specific
            for (use in this) {
                if (parentMatchLine != null && use.item.line < parentMatchLine) break
                if (use.rule.isMatch(path, isDirectory)) {
                    use.used = true
                    return use
                }
            }
            return parentMatch
        }

        fun findMatchLine(path: String, isDirectory: Boolean, parentMatch: ItemUse?): ItemUse? {
            return if (isDirectory) {
                matchers.findFirstMatching(path, isDirectory = true, parentMatch)
            } else {
                fileMatchers.findFirstMatching(path, isDirectory = false, parentMatch)
            }
        }

        fun visitFile(file: File, parentMatch: ItemUse?) {
            val path = file.path.replace(File.separatorChar, '/')
            if (ignoreTracker.isIgnored(path, isDirectory = false)) return

            val matchedItem = findMatchLine(path, isDirectory = false, parentMatch)
            if (matchedItem != fallbackMatcher) return
            if (unmatchedFilesTop.size < 10) {
                unmatchedFilesTop.add(file)
            }
        }

        fun visitDirectory(directory: File, parentMatch: ItemUse?, depth: Int) {
            val path = directory.path.replace(File.separatorChar, '/')

            if (ignoreTracker.isIgnored(path, isDirectory = true)) return
            val directoryMatch = findMatchLine(path, isDirectory = true, parentMatch)
            ignoreTracker.withDirectory(directory) {
                for (childName in (directory.list() ?: emptyArray())) {
                    val child = if (directory == root) {
                        File(childName)
                    } else {
                        File(directory, childName)
                    }
                    if (child.isDirectory) {
                        visitDirectory(child, directoryMatch, depth + 1)
                    } else {
                        visitFile(child, directoryMatch)
                    }
                }
            }
        }

        fun check() {
            visitDirectory(root, null, 0)
        }

        fun unusedMatchers(): List<ItemUse> {
            return matchers.filterNot { it.used }
        }
    }
}


private class GitIgnoreTracker {
    private val ignoreNodeStack = mutableListOf(
        IgnoreNode(listOf(FastIgnoreRule("/.git")))
    )
    private val reversedIgnoreNodeStack = ignoreNodeStack.asReversed()

    fun isIgnored(path: String, isDirectory: Boolean): Boolean {
        return reversedIgnoreNodeStack.firstNotNullOfOrNull { ignoreNode -> ignoreNode.checkIgnored(path, isDirectory) } ?: false
    }

    inline fun withDirectory(directory: File, action: () -> Unit) {
        val ignoreFile = directory.resolve(".gitignore").takeIf { it.exists() }
        if (ignoreFile != null) {
            val ignoreNode = IgnoreNode().apply {
                ignoreFile.inputStream().use {
                    parse(ignoreFile.path, ignoreFile.inputStream())
                }
            }
            ignoreNodeStack.add(ignoreNode)
        }
        action()
        if (ignoreFile != null) {
            ignoreNodeStack.removeAt(ignoreNodeStack.lastIndex)
        }
    }
}

private data class CodeOwners(
    val permittedOwners: List<OwnerListEntry>,
    val userOwners: List<UserOwnerEntry>,
    val githubOwners: List<GitHubOwnerEntry>,
    val patterns: List<OwnershipPattern>,
    val branchRules: List<BranchRule> = emptyList()
) {
    data class OwnerListEntry(val name: String, val line: Int) {
        override fun toString(): String {
            return "line $line |# $SPACE_OWNER_DIRECTIVE: $name"
        }
    }

    data class UserOwnerEntry(val email: String, val githubUsername: String, val line: Int) {
        override fun toString(): String {
            return "line $line |# $USER_OWNER_DIRECTIVE: $email $githubUsername"
        }
    }

    data class GitHubOwnerEntry(val teamName: String, val usernames: List<String>, val line: Int) {
        override fun toString(): String {
            return "line $line |# $SPACE_TO_GITHUB_OWNER_DIRECTIVE: $teamName ${usernames.joinToString(" ")}"
        }
    }
}

private data class BranchRule(val branchPattern: String, val pathPattern: String, val owners: List<String>, val line: Int) {
    override fun toString(): String {
        return "line $line |# $BRANCH_RULE_DIRECTIVE: $branchPattern $pathPattern ${owners.joinToString(" ") { it.quoteIfContainsSpaces() }}"
    }
}

private sealed class OwnershipPattern {
    abstract val line: Int
    abstract val pattern: String

    data class Pattern(override val pattern: String, val owners: List<String>, override val line: Int) : OwnershipPattern() {
        override fun toString(): String {
            return "line $line |$pattern " + owners.joinToString(separator = " ") { it.quoteIfContainsSpaces() }
        }
    }

    data class UnknownPathPattern(override val pattern: String, override val line: Int) : OwnershipPattern() {
        override fun toString(): String {
            return "line $line |# $UNKNOWN_DIRECTIVE: $pattern"
        }
    }

    data class NoOwnerPattern(override val pattern: String, override val line: Int) : OwnershipPattern() {
        override fun toString(): String {
            return "line $line |# $NO_OWNER_DIRECTIVE: $pattern"
        }
    }
}

private fun String.quoteIfContainsSpaces() = if (contains(' ')) "\"$this\"" else this

private fun parseCodeOwners(file: File): CodeOwners {
    fun parseDirective(line: String, directive: String): String? {
        val value = line.substringAfter("# $directive: ")
        if (value != line) return value
        return null
    }

    val ownersPattern = "(\"[^\"]+\")|(\\S+)".toRegex()

    fun parseOwnerNames(ownerString: String): List<String> {
        return ownersPattern.findAll(ownerString).map { it.value }.toList()
    }

    val permittedOwners = mutableListOf<CodeOwners.OwnerListEntry>()
    val userOwners = mutableListOf<CodeOwners.UserOwnerEntry>()
    val githubOwners = mutableListOf<CodeOwners.GitHubOwnerEntry>()
    val patterns = mutableListOf<OwnershipPattern>()
    val excludedPatterns = mutableListOf<OwnershipPattern.NoOwnerPattern>()
    val branchRules = mutableListOf<BranchRule>()

    file.useLines { lines ->

        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1

            if (line.startsWith("#")) {
                val unknownDirective = parseDirective(line, UNKNOWN_DIRECTIVE)
                if (unknownDirective != null) {
                    patterns += OwnershipPattern.UnknownPathPattern(unknownDirective.trim(), lineNumber)
                    continue
                }

                val spaceOwnerDirective = parseDirective(line, SPACE_OWNER_DIRECTIVE)
                if (spaceOwnerDirective != null) {
                    parseOwnerNames(spaceOwnerDirective).mapTo(permittedOwners) { owner ->
                        CodeOwners.OwnerListEntry(owner, lineNumber)
                    }
                }

                val userOwnerDirective = parseDirective(line, USER_OWNER_DIRECTIVE)
                if (userOwnerDirective != null) {
                    val parts = userOwnerDirective.trim().split("\\s+".toRegex(), limit = 2)
                    if (parts.size == 2) {
                        val (email, githubUsername) = parts
                        userOwners += CodeOwners.UserOwnerEntry(email, githubUsername, lineNumber)
                        permittedOwners += CodeOwners.OwnerListEntry(email, lineNumber)
                    }
                }

                val githubOwnerDirective = parseDirective(line, SPACE_TO_GITHUB_OWNER_DIRECTIVE)
                if (githubOwnerDirective != null) {
                    val parsed = parseOwnerNames(githubOwnerDirective)
                    if (parsed.isNotEmpty()) {
                        val teamName = parsed.first()
                        val usernames = parsed.drop(1)
                        githubOwners += CodeOwners.GitHubOwnerEntry(teamName, usernames, lineNumber)
                        // Also add the team to permitted owners (no need for separate SPACE_OWNER)
                        permittedOwners += CodeOwners.OwnerListEntry(teamName, lineNumber)
                    }
                }

                val noOwnerDirective = parseDirective(line, NO_OWNER_DIRECTIVE)
                if (noOwnerDirective != null) {
                    excludedPatterns += OwnershipPattern.NoOwnerPattern(noOwnerDirective.trim(), lineNumber)
                }

                val branchRuleDirective = parseDirective(line, BRANCH_RULE_DIRECTIVE)
                if (branchRuleDirective != null) {
                    val parts = branchRuleDirective.trim().split(' ', limit = 3)
                    if (parts.size >= 3) {
                        val branchPattern = parts[0]
                        val pathPattern = parts[1]
                        val ruleOwners = parseOwnerNames(parts[2])
                        branchRules += BranchRule(branchPattern, pathPattern, ruleOwners, lineNumber)
                    }
                }
            } else if (line.isNotBlank() && line !in excludedPatterns.map { it.pattern }) {
                // Note: Space CODEOWNERS grammar is ambiguous, as it is impossible to distinguish between file pattern with spaces
                // and team name, so we re-use similar logic
                // ex:
                // ```
                // /some/path/Read Me.md Owner
                // ```
                // In such pattern it is impossible to distinguish between file ".../Read Me.md" or file ".../Read" owned by "Me.md"
                // See SPACE-17772
                val (pattern, owners) = line.split(' ', limit = 2)
                patterns += OwnershipPattern.Pattern(pattern, parseOwnerNames(owners), lineNumber)
            }
        }
    }

    return CodeOwners(permittedOwners, userOwners, githubOwners, patterns, branchRules)
}

private const val SPACE_OWNER_DIRECTIVE = "SPACE_OWNER"
private const val USER_OWNER_DIRECTIVE = "USER_OWNER"
private const val SPACE_TO_GITHUB_OWNER_DIRECTIVE = "SPACE_TO_GITHUB_OWNER"
private const val UNKNOWN_DIRECTIVE = "UNKNOWN"
private const val NO_OWNER_DIRECTIVE = "NO_OWNER"
private const val BRANCH_RULE_DIRECTIVE = "GITHUB_BRANCH_RULE"
