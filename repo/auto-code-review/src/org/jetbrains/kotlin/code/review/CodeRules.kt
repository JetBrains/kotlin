/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

import org.eclipse.jgit.ignore.FastIgnoreRule

data class CodeRule(
    val name: String,
    val text: String,
    val patterns: CodeRulePatterns,
    val source: ProjectFilePath,
) {
    fun patternsMatch(path: ProjectFilePath): Boolean {
        // If the path is within the rule source directory, check the relative path:
        source.dir.relativePathToFileInside(path)?.let {
            if (patterns.match(it)) return true
        }

        // In any case, check the path from the root of the project.
        // This way we can use patterns in combination with rule file includes.
        return patterns.match(path.pathFromProjectRoot)
    }
}

data class CodeRulePatterns(val patterns: List<String>) {
    // Use JGit as the implementation detail:
    private val fastIgnoreRules = patterns.map { FastIgnoreRule(it) }

    fun match(path: String): Boolean {
        if (fastIgnoreRules.isEmpty()) return true

        // Reverse to match the `.gitignore` behavior.
        fastIgnoreRules.reversed().forEach {
            // `it.result` is `true` for regular patterns and `false` for negative patterns (`!`).
            if (it.isMatch(path, /* directory = */ false)) return it.result
        }
        return false
    }
}

const val CODE_RULES_MD = "code-rules.md"
private const val INCLUDE_PREFIX = "@"
private const val RULE_NAME_PREFIX = "# "
private const val PATTERN_PREFIX = "Pattern:"

class CodeRuleRepository(val project: Project) {
    suspend fun getRules(path: ProjectFilePath): Set<CodeRule> {
        val rulesFile = path.dir.file(CODE_RULES_MD)
        return getRulesFromRulesFile(rulesFile)
            .filterTo(mutableSetOf()) { it.patternsMatch(path) }
    }

    private val ruleFileToRules = mutableMapOf<ProjectFilePath, Set<CodeRule>>()
    internal suspend fun getRulesFromRulesFile(rulesFile: ProjectFilePath) =
        ruleFileToRules.getOrPut(rulesFile) {
            buildSet {
                // Note: this is suboptimal (we could have shared the DFS state across all requests).
                // But it is not a hot path, and this approach keeps things simple and handles cycles naturally.
                dfs(
                    node = parseRulesFile(rulesFile),
                    getNeighbors = { includes.map { parseRulesFile(it) } },
                    onVisit = { addAll(it.rules) }
                )
            }
        }

    private class ParsedRulesFile(val includes: List<ProjectFilePath>, val rules: List<CodeRule>)

    private val parsedRulesFiles = mutableMapOf<ProjectFilePath, ParsedRulesFile>()
    private suspend fun parseRulesFile(file: ProjectFilePath): ParsedRulesFile =
        parsedRulesFiles.getOrPut(file) { parseRulesFileImpl(file) }

    private suspend fun parseRulesFileImpl(file: ProjectFilePath): ParsedRulesFile {
        val dir = file.dir

        val lines = ArrayDeque(project.readLines(file).orEmpty())

        val includes = buildList {
            // `foo/bar/baz.md` includes `foo/baz.md`:
            dir.parent?.let { add(it.file(file.fileName)) }

            lines.dropFirstBlankLines()

            while (lines.firstOrNull()?.startsWith(INCLUDE_PREFIX) == true) {
                val include = lines.removeFirst().removePrefix(INCLUDE_PREFIX)
                val includedFile = if (include.startsWith("/"))
                    ProjectFilePath(include.removePrefix("/"))
                else {
                    dir.file(include)
                }
                check(project.fileExists(includedFile)) {
                    "$file includes ($INCLUDE_PREFIX) non-existing $includedFile"
                }
                add(includedFile)

                lines.dropFirstBlankLines()
            }
        }

        lines.forEach {
            check(!it.startsWith(INCLUDE_PREFIX)) {
                """
                    $file contains includes ($INCLUDE_PREFIX) not at the beginning of the file:
                    $it
                """.trimIndent()
            }
        }

        val rules = mutableListOf<CodeRule>()

        while (lines.isNotEmpty()) {
            val firstLine = lines.first()
            check(firstLine.startsWith(RULE_NAME_PREFIX)) {
                """
                    |In $file,
                    |expected a rule name starting with `$RULE_NAME_PREFIX`, but got:
                    |$firstLine
                """.trimMargin()
            }

            val ruleLines = listOf(lines.removeFirst()) +
                    lines.removeFirstUntil { it.startsWith(RULE_NAME_PREFIX) }

            rules.add(parseRule(ruleLines, file))
        }

        return ParsedRulesFile(includes, rules)
    }

    private fun parseRule(lines: List<String>, source: ProjectFilePath): CodeRule {
        val lines = ArrayDeque(lines)

        val name = lines.removeFirst().removePrefix(RULE_NAME_PREFIX)

        lines.dropFirstBlankLines()

        val patterns = buildList {
            while (lines.firstOrNull()?.startsWith(PATTERN_PREFIX) == true) {
                add(lines.removeFirst().removePrefix(PATTERN_PREFIX).trim())
                lines.dropFirstBlankLines()
            }
        }

        lines.dropLastBlankLines()

        val text = lines.joinToString("\n")

        return CodeRule(
            name = name,
            text = text,
            patterns = CodeRulePatterns(patterns),
            source = source
        )
    }

}

private fun ArrayDeque<String>.dropFirstBlankLines() {
    while (this.firstOrNull()?.isBlank() == true) {
        this.removeFirst()
    }
}

private fun ArrayDeque<String>.dropLastBlankLines() {
    while (this.lastOrNull()?.isBlank() == true) {
        this.removeLast()
    }
}

private suspend fun <N> dfs(
    node: N,
    getNeighbors: suspend N.() -> List<N>,
    onVisit: suspend (N) -> Unit,
) {
    val visited = mutableSetOf<N>()

    suspend fun doDfs(current: N) {
        if (!visited.add(current)) return
        onVisit(current)

        for (neighbor in current.getNeighbors()) {
            doDfs(neighbor)
        }
    }

    doDfs(node)
}
