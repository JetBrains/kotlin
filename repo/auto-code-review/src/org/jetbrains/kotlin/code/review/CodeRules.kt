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
private const val EXCLUSION_PATTERN_PREFIX = "!"

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

            rules.add(CodeRuleParser.parseRule(ruleLines, file))
        }

        return ParsedRulesFile(includes, rules)
    }
}

internal object CodeRuleParser {
    private const val APPLIES_TO_LABEL = "Applies to:"
    private const val CODE_SPAN_DELIMITER = "`"
    private const val CODE_FENCE = "```"

    fun parseRule(lines: List<String>, source: ProjectFilePath): CodeRule {
        val lines = ArrayDeque(lines)

        val name = lines.removeFirst().removePrefix(RULE_NAME_PREFIX)
        val ruleLocation = """$source, rule "$name""""

        lines.dropFirstBlankLines()

        val patterns = if (lines.firstOrNull()?.startsWith(APPLIES_TO_LABEL) == true) {
            parseAppliesTo(lines, ruleLocation).also { lines.dropFirstBlankLines() }
        } else {
            emptyList()
        }

        lines.forEach {
            check(!it.startsWith(APPLIES_TO_LABEL)) {
                """
                    |In $ruleLocation,
                    |`$APPLIES_TO_LABEL` is allowed only right after the rule name, but got:
                    |$it
                """.trimMargin()
            }
        }

        // With exclusion patterns going last, the `.gitignore`-like matching
        // doesn't depend on the order: a file is matched if it matches any regular pattern and no exclusion pattern.
        // This is an artificial restriction to make the model more understandable.
        // `.gitignore` supports arbitrary order, and the implementation in `CodeRulePatterns.match` supports it too.
        patterns.zipWithNext().forEach { [previous, next] ->
            check(!previous.startsWith(EXCLUSION_PATTERN_PREFIX) || next.startsWith(EXCLUSION_PATTERN_PREFIX)) {
                """
                    |In $ruleLocation,
                    |exclusion patterns ($EXCLUSION_PATTERN_PREFIX) must go after all other patterns, but got
                    |$next
                    |after
                    |$previous
                """.trimMargin()
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

    /**
     * Parses the patterns defined either as
     * ```
     * Applies to: `pattern`
     * ```
     * or as
     * ````
     * Applies to:
     * ```
     * pattern1
     * pattern2
     * ```
     * ````
     */
    private fun parseAppliesTo(lines: ArrayDeque<String>, ruleLocation: String): List<String> {
        val labelLine = lines.removeFirst()
        val value = labelLine.removePrefix(APPLIES_TO_LABEL).trim()

        if (value.isNotEmpty()) {
            val pattern = value.removePrefix(CODE_SPAN_DELIMITER).removeSuffix(CODE_SPAN_DELIMITER)
            check(
                value.startsWith(CODE_SPAN_DELIMITER) && value.endsWith(CODE_SPAN_DELIMITER) &&
                        pattern.isNotBlank() && CODE_SPAN_DELIMITER !in pattern
            ) {
                """
                    |In $ruleLocation,
                    |expected `$APPLIES_TO_LABEL` to be followed by a single pattern in backticks
                    |or by a code block with patterns on the next lines, but got:
                    |$labelLine
                """.trimMargin()
            }
            return listOf(pattern)
        }

        lines.dropFirstBlankLines()
        val openingFence = lines.removeFirstOrNull()
        check(openingFence?.startsWith(CODE_FENCE) == true) {
            """
                |In $ruleLocation,
                |expected a code block with patterns ($CODE_FENCE) right after `$APPLIES_TO_LABEL`, but got:
                |${openingFence.orEmpty()}
            """.trimMargin()
        }

        val patterns = buildList {
            while (true) {
                val line = checkNotNull(lines.removeFirstOrNull()) {
                    "In $ruleLocation, the code block with patterns is not closed ($CODE_FENCE)"
                }
                if (line.trim() == CODE_FENCE) break
                if (line.isNotBlank()) add(line)
            }
        }

        check(patterns.isNotEmpty()) {
            "In $ruleLocation, the code block with patterns is empty"
        }

        return patterns
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
