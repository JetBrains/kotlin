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
    private val ownersFile = File(".space/CODEOWNERS")
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

    fun testAllOwnersInOwnerList() {
        val permittedOwnerNames = owners.permittedOwners.map { it.name }.toSet()
        val problems = mutableListOf<String>()
        for (pattern in owners.patterns) {
            if (pattern !is OwnershipPattern.Pattern) continue
            for (owner in pattern.owners) {
                if (owner !in permittedOwnerNames) {
                    problems += "Owner ${owner.quoteIfContainsSpaces()} not listed in OWNER_LIST of $ownersFile, but used in $pattern"
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
    val patterns: List<OwnershipPattern>
) {
    data class OwnerListEntry(val name: String, val line: Int) {
        override fun toString(): String {
            return "line $line |# $OWNER_LIST_DIRECTIVE: $name"
        }
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
        return ownersPattern.findAll(ownerString).map { it.value.removeSurrounding("\"") }.toList()
    }

    val permittedOwners = mutableListOf<CodeOwners.OwnerListEntry>()
    val patterns = mutableListOf<OwnershipPattern>()

    file.useLines { lines ->

        for ((index, line) in lines.withIndex()) {
            val lineNumber = index + 1
            if (line.startsWith("#")) {
                val unknownDirective = parseDirective(line, UNKNOWN_DIRECTIVE)
                if (unknownDirective != null) {
                    patterns += OwnershipPattern.UnknownPathPattern(unknownDirective.trim(), lineNumber)
                    continue
                }

                val ownerListDirective = parseDirective(line, OWNER_LIST_DIRECTIVE)
                if (ownerListDirective != null) {
                    parseOwnerNames(ownerListDirective).mapTo(permittedOwners) { owner ->
                        CodeOwners.OwnerListEntry(owner, lineNumber)
                    }
                }
            } else if (line.isNotBlank()) {
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

    return CodeOwners(permittedOwners, patterns)
}

private const val OWNER_LIST_DIRECTIVE = "OWNER_LIST"
private const val UNKNOWN_DIRECTIVE = "UNKNOWN"
