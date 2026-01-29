/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

/**
 * Build a string with indentable blocks.
 *
 * Use [StringBlockBuilder.line] to add a line.
 *
 * Use [StringBlockBuilder.block] to add an indented block.
 *
 * @param[defaultIndent] Each block will be indented with this string.
 */
@StringBlockBuilderDsl
internal fun buildStringBlock(
    defaultIndent: String = "    ",
    block: StringBlockBuilder.() -> Unit,
): String {
    val builder = StringBlockBuilderImpl(
        defaultIndent = defaultIndent
    )
    builder.apply(block)
    return builder.render()
}

/**
 * Build a string of connected lines.
 *
 * The first line will be added to the current level.
 * The other lines will be indented.
 *
 * All lines, except the last, will be suffixed with [lineSuffix].
 *
 * ###### Example: Create a multiline bash command.
 *
 * ```kotlin
 * connectedLines(" \\") {
 *   line("execute_command")
 *   line("-a /path/to/file")
 *   line("-b another_option")
 *   line("-c third_option")
 * }
 * ```
 *
 * Result:
 *
 * ```text
 * execute_command \
 *     -a /path/to/file \
 *     -b another_option \
 *     -c third_option
 * ```
 */
@StringBlockBuilderDsl
internal fun StringBlockBuilder.connectedLines(
    lineSuffix: String,
    block: ConnectedLinesBuilder.() -> Unit,
) {
    check(this is StringBlockBuilderImpl)
    val builder = ConnectedLinesBuilderImpl(
        lineSuffix = lineSuffix,
        level = this.level,
    )
    builder.block()
    lines += builder.lines
}


/**
 * @see buildStringBlock
 */
@StringBlockBuilderDsl
internal sealed interface StringBlockBuilder {

    /**
     * Add a line to the current block.
     */
    @StringBlockBuilderDsl
    fun line(content: String = "")

    /**
     * Starts a new indented block, surrounded by [open] and [close].
     *
     * All content added in [content] will be intended one level more than the current indentation.
     *
     * [open] and [close] will _not_ be indented.
     */
    @StringBlockBuilderDsl
    fun block(open: String, close: String, content: StringBlockBuilder.() -> Unit)
}

/**
 * @see connectedLines
 */
@StringBlockBuilderDsl
internal sealed interface ConnectedLinesBuilder {
    /**
     * Add a new line, continuing from any previously added lines.
     *
     * If this is the first line, it will not be indented.
     * Otherwise, it will be indented once.
     *
     * All lines, except the last, will be suffixed with the suffix set in [connectedLines].
     */
    fun line(content: String)
}

/**
 * Build a block of comma-separated entries.
 *
 * Each entry can contain multiple lines and/or nested blocks.
 * A comma is appended to the **last line** of each entry, **except the final entry**.
 *
 * ###### Example: Create a Swift Package.swift manifest.
 *
 * ```kotlin
 * block("let package = Package(", ")") {
 *     commaSeparatedEntries {
 *         entry { line("name: \"MyPackage\"") }
 *         entry { block("platforms: [", "]") { emitListItems(platforms) } }
 *         entry { block("products: [", "]") { ... } }
 *     }
 * }
 * ```
 *
 * Result:
 *
 * ```text
 * let package = Package(
 *     name: "MyPackage",
 *     platforms: [
 *         .iOS("15.0")
 *     ],
 *     products: [
 *         ...
 *     ]
 * )
 * ```
 */
@StringBlockBuilderDsl
internal fun StringBlockBuilder.commaSeparatedEntries(
    block: CommaSeparatedEntriesBuilder.() -> Unit,
) {
    check(this is StringBlockBuilderImpl)
    val builder = CommaSeparatedEntriesBuilderImpl(
        level = this.level,
        defaultIndent = this.defaultIndent,
    )
    builder.block()
    lines += builder.buildLines()
}

/**
 * @see commaSeparatedEntries
 */
@StringBlockBuilderDsl
internal sealed interface CommaSeparatedEntriesBuilder {
    /**
     * Add an entry to the comma-separated list.
     *
     * Each entry can contain multiple lines and/or nested blocks.
     * A comma will be appended to the last line of this entry,
     * unless it is the final entry in the list.
     */
    @StringBlockBuilderDsl
    fun entry(content: StringBlockBuilder.() -> Unit)
}

private class StringBlockBuilderImpl(
    val level: Int = 0,
    val defaultIndent: String = "    ",
) : StringBlockBuilder {
    val lines: ArrayDeque<CodeLine> = ArrayDeque()

    override fun line(
        content: String,
    ) {
        lines.addLast(CodeLine(content, level))
    }

    override fun block(
        open: String,
        close: String,
        content: StringBlockBuilder.() -> Unit,
    ) {
        val builder = StringBlockBuilderImpl(level + 1)
        builder.apply(content)
        if (open.isNotEmpty()) line(open)
        lines += builder.lines
        if (close.isNotEmpty()) line(close)
    }

    fun render(): String {
        return lines
            .joinToString("\n") { line ->
                line.content
                    .prependIndent(defaultIndent.repeat(line.level))
                    .trimEnd()
            }
            .trimEnd()
            .let {
                if (it.isBlank()) it else "$it\n"
            }
    }
}

private class ConnectedLinesBuilderImpl(
    private val lineSuffix: String,
    private val level: Int,
) : ConnectedLinesBuilder {
    val lines: ArrayDeque<CodeLine> = ArrayDeque()

    override fun line(content: String) {
        if (lines.isEmpty()) {
            lines.addLast(CodeLine(content, level))
        } else {
            val lastLine = lines.last()
            lines.removeLast()
            lines.addLast(CodeLine(lastLine.content + lineSuffix, lastLine.level))
            lines.addLast(CodeLine(content, level + 1))
        }
    }
}

private class CommaSeparatedEntriesBuilderImpl(
    private val level: Int,
    private val defaultIndent: String,
) : CommaSeparatedEntriesBuilder {
    private val entries: MutableList<ArrayDeque<CodeLine>> = mutableListOf()

    override fun entry(content: StringBlockBuilder.() -> Unit) {
        val entryBuilder = StringBlockBuilderImpl(level = level, defaultIndent = defaultIndent)
        entryBuilder.content()
        entries.add(entryBuilder.lines)
    }

    fun buildLines(): ArrayDeque<CodeLine> {
        val result = ArrayDeque<CodeLine>()
        entries.forEachIndexed { index, entryLines ->
            val isLastEntry = index == entries.lastIndex
            if (!isLastEntry && entryLines.isNotEmpty()) {
                // Add comma to the last line of this entry
                val lastLine = entryLines.removeLast()
                entryLines.addLast(CodeLine(lastLine.content + ",", lastLine.level))
            }
            result.addAll(entryLines)
        }
        return result
    }
}

private data class CodeLine(
    val content: String,
    val level: Int,
)

@DslMarker
@MustBeDocumented
internal annotation class StringBlockBuilderDsl

/**
 * Emit a list of items, each potentially multi-line, with proper comma separation.
 * Items are separated by commas, with no trailing comma after the last item.
 */
@StringBlockBuilderDsl
internal fun StringBlockBuilder.emitListItems(items: List<String>) {
    items.forEachIndexed { index, item ->
        val isLast = index == items.lastIndex
        val lines = item.lines()
        lines.forEachIndexed { lineIndex, lineContent ->
            val isLastLine = lineIndex == lines.lastIndex
            if (isLastLine && !isLast) {
                line("$lineContent,")
            } else {
                line(lineContent)
            }
        }
    }
}
