/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.explain

public abstract class Explanation internal constructor() {
    // TODO create another class to hold offset and source? CodeBlock?
    // TODO include file name?
    public abstract val offset: Int // Always starts at *column* 0 within the file.
    public abstract val source: String // The *block* of source code, redacted with whitespace.
    public abstract val expressions: List<Expression>
}

// DFS of explanations
// TODO rename to getChildren?
public fun Explanation.getSubExplanations(): List<Explanation> {
    fun MutableList<Explanation>.addChildren(parent: Explanation) {
        for (expression in parent.expressions) {
            if (expression is ExplainedExpression) {
                val child = expression.explanation
                add(child)
                addChildren(child)
            }
        }
    }

    return buildList { addChildren(this@getSubExplanations) }
}

// Explanation diagram builders

public fun Explanation.toDefaultMessage(
    render: Expression.() -> String? = Expression::render,
): String = buildString {
    appendLine(toDiagram(render))
    appendExplanations(this@toDefaultMessage, render)
}

public fun Explanation.toDiagram(
    render: Expression.() -> String? = Expression::render,
): String {
    val diagram = buildString { appendDiagram(source, expressions, render) }
    if (this is VariableExplanation) {
        // Clear everything before the name of the variable to keep the diagram consistent.
        val prefix = diagram
            .substring(0, initializer.startOffset)
            .substringBeforeLast(name, missingDelimiterValue = "")
        // TODO check for surrounding backticks?
        return diagram.replaceFirst(prefix, prefix.replace("\\S".toRegex(), " ")).trimIndent()
    }
    return diagram.trimIndent()
}

private fun StringBuilder.appendDiagram(
    source: String,
    expressions: List<Expression>,
    render: Expression.() -> String?,
) {
    class DiagramValue(
        val row: Int,
        val indent: Int,
        val display: String,
    ) {
        fun overlaps(other: DiagramValue): Boolean {
            return row == other.row && indent < other.indent && display.length >= (other.indent - indent)
        }
    }

    val newlineOffsets = buildList {
        source.forEachIndexed { index, c -> if (c == '\n') add(index) }
    }

    fun Expression.toDiagramValue(): DiagramValue? {
        val display = render() ?: return null
        val row = -(newlineOffsets.binarySearch(displayOffset) + 1)
        val rowOffset = if (row == 0) 0 else newlineOffsets[row - 1] + 1
        return DiagramValue(
            row = row,
            indent = displayOffset - rowOffset,
            display = display,
        )
    }

    val valuesByRow = expressions.sortedBy { it.displayOffset }
        .mapNotNull { it.toDiagramValue() }
        .groupBy { it.row }

    var separationNeeded = false
    for ((rowIndex, rowSource) in source.split("\n").withIndex()) {
        // Add an extra blank line if needed between values and source code.
        if (separationNeeded && rowSource.isNotBlank()) appendLine()
        separationNeeded = false

        appendLine(rowSource)

        val rowValues = valuesByRow[rowIndex] ?: continue
        separationNeeded = true // Row contains values, so separate it from the next line of source.

        fun appendWithIndent(currentIndent: Int, indent: Int, str: String): Int {
            for (i in currentIndent..<indent) {
                when (rowSource[i]) {
                    '\t' -> append('\t')
                    else -> append(' ')
                }
            }
            append(str)
            return indent + str.length
        }

        // Print the first row of displayable indicators.
        run {
            var currentIndent = 0
            for (indent in rowValues.map { it.indent }) {
                currentIndent = appendWithIndent(currentIndent, indent, "|")
            }
            appendLine()
        }

        // To maintain correct indentation of value renders,
        // if the span of a value render covers a tab character in the row source,
        // it may not always be displayable with other values.
        // Precalculate all values that span a tab so they can be excluded as needed.
        val valuesCoveringTab = mutableSetOf<DiagramValue>()
        for (value in rowValues) {
            if ('\t' in rowSource.substring(value.indent, minOf(rowSource.length, value.indent + value.display.length))) {
                valuesCoveringTab.add(value)
            }
        }

        val remaining = rowValues.toMutableList()
        while (remaining.isNotEmpty()) {
            // Figure out which displays will fit on this row.
            val displayRow = remaining.windowed(2, partialWindows = true)
                .filter { it.size == 1 || it[0] !in valuesCoveringTab && !it[0].overlaps(it[1]) }
                .mapTo(mutableSetOf()) { it[0] }

            var currentIndent = 0
            for (diagramValue in remaining) {
                currentIndent = appendWithIndent(
                    currentIndent = currentIndent,
                    indent = diagramValue.indent,
                    str = if (diagramValue in displayRow) diagramValue.display else "|",
                )
            }
            appendLine()

            remaining -= displayRow
        }
    }
}

private fun StringBuilder.appendExplanations(
    parent: Explanation,
    render: Expression.() -> String?,
) {
    val children = parent.getSubExplanations().sortedBy { -it.offset }
    for ((index, child) in children.withIndex()) {
        appendLine()
        appendLine(if (index == 0) "Where:" else "And:")
        appendLine(child.toDiagram(render))
    }
}

// TODO service loader implementation?
public fun Expression.render(): String {
    if (this is EqualityExpression && value == false) {
        return "Expected <${lhs.render()}>, actual <${rhs.render()}>."
    }

    return value.render()
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun Any?.render(): String {
    return when (val value = this) {
        is Array<*> -> value.contentDeepToString()
        is ByteArray -> value.contentToString()
        is ShortArray -> value.contentToString()
        is IntArray -> value.contentToString()
        is LongArray -> value.contentToString()
        is BooleanArray -> value.contentToString()
        is CharArray -> value.contentToString()
        is UByteArray -> value.contentToString()
        is UShortArray -> value.contentToString()
        is UIntArray -> value.contentToString()
        is ULongArray -> value.contentToString()
        else -> value.toString()
    }
}
