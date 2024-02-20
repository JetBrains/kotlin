package komem.litmus

fun List<List<String>>.tableFormat(hasHeader: Boolean = false): String {
    val columnCount = maxOf { it.size }
    val columnSizes = (0..<columnCount).map { i ->
        this.mapNotNull { it.getOrNull(i) }.maxOf { it.length } + 2
    }
    return buildString {
        this@tableFormat.forEach { row ->
            row.forEachIndexed { i, word ->
                val startPadding = (columnSizes[i] - word.length) / 2
                val endPadding = columnSizes[i] - word.length - startPadding
                append(" ".repeat(startPadding))
                append(word)
                append(" ".repeat(endPadding))
                if (i != row.size - 1) append("|")
            }
            appendLine()
            if (hasHeader && row === this@tableFormat.first()) {
                appendLine("-".repeat(columnSizes.sum() + columnCount - 1))
            }
        }
    }
}

expect fun cpuCount(): Int
