/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.powerassert

/**
 * Given an [Explanation], builds the default Power-Assert style diagram.
 * Used to build the string argument for Power-Assert transformations of
 * functions provided via fully-qualified domain names.
 */
// TODO toDefaultDiagramString?
@ExperimentalPowerAssert
public fun Explanation.toDefaultMessage(
    render: (Expression) -> String? = Expression::valueToString,
): String = buildString {
    appendDiagram(source, expressions, render)
    appendLine()
}.trimIndent()

@ExperimentalPowerAssert
private fun StringBuilder.appendDiagram(
    source: String,
    expressions: List<Expression>,
    render: (Expression) -> String?,
) {
    class DiagramValue(
        val row: Int,
        val indent: Int,
        val display: String,
    ) {
        fun overlaps(other: DiagramValue): Boolean {
            return row == other.row && indent <= other.indent && display.length >= (other.indent - indent)
        }
    }

    val newlineOffsets = buildList {
        source.forEachIndexed { index, c -> if (c == '\n') add(index) }
    }

    fun Expression.toDiagramValue(): DiagramValue? {
        if (this is LiteralExpression) return null
        val display = render(this) ?: return null
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

@ExperimentalPowerAssert
@OptIn(ExperimentalUnsignedTypes::class)
private fun Expression.valueToString(): String {
    return when (val value = value) {
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
