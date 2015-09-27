@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin

/**
 * Trims leading whitespace characters followed by [marginPrefix] from every line of a source string and removes
 * first and last lines if they are blank (notice difference blank vs empty). Doesn't affect line if it doesn't contain
 * [marginPrefix] except first and last blank lines
 *
 * Do not preserves original line endings
 *
 * [marginPrefix] should be non-blank string
 *
 * Example
 * ```kotlin
 * assertEquals("ABC\n123\n456", """ABC
 *                             |123
 *                             |456""".trimMargin())
 * ```
 *
 * @param marginPrefix characters to be used as a margin delimiter. Default is `|` (pipe character)
 * @return the trimmed String
 * @see kotlin.isWhitespace
 * @since M13
 */
public fun String.trimMargin(marginPrefix: String = "|"): String =
    replaceIndentByMargin("", marginPrefix)

/**
 * Detects indent by [marginPrefix] as it does [trimMargin] and replace it with [newIndent]
 */
public fun String.replaceIndentByMargin(newIndent: String = "", marginPrefix: String = "|"): String {
    require(marginPrefix.isNotBlank()) { "marginPrefix should be non blank string but it is '$marginPrefix'" }
    val lines = lines()

    return lines.reindent(length() + newIndent.length() * lines.size(), getIndentFunction(newIndent)) { line ->
        val firstNonWhitespaceIndex = line.indexOfFirst { !it.isWhitespace() }

        when {
            firstNonWhitespaceIndex == -1 -> null
            line.startsWith(marginPrefix, firstNonWhitespaceIndex) -> line.substring(firstNonWhitespaceIndex + marginPrefix.length())
            else -> null
        }
    }
}

/**
 * Detects a common minimal indent of all the input lines, removes it from every line and also removes first and last
 * lines if they are blank (notice difference blank vs empty).
 * Note that blank lines do not affect detected indent level.
 * Please keep in mind that if there are non-blank lines with no leading whitespace characters (no indent at all) then the
 * common indent is 0 so this function may do nothing so it is recommended to keep first line empty (will be dropped).
 *
 * Do not preserves original line endings
 *
 * Example
 * ```kotlin
 * assertEquals("ABC\n123\n456", """
 *                             ABC
 *                             123
 *                             456""".trimMargin())
 * ```
 *
 * @return deindented String
 * @see kotlin.isBlank
 * @since M13
 */
public fun String.trimIndent(): String = replaceIndent("")

/**
 * Detects a common minimal indent like it does [trimIndent] and replaces it with the specified [newIndent]
 * @since M13
 */
public fun String.replaceIndent(newIndent: String = ""): String {
    val lines = lines()

    val minCommonIndent = lines
            .filter { it.isNotBlank() }
            .map { it.indentWidth() }
            .min() ?: 0

    return lines.reindent(length() + newIndent.length() * lines.size(), getIndentFunction(newIndent)) { line -> line.drop(minCommonIndent) }
}

/**
 * Prepends [indent] to every line of the original string. Does not preserve original line endings
 */
public fun String.prependIndent(indent: String = "    "): String =
    lineSequence()
    .map {
        when {
            it.isBlank() -> {
                when {
                    it.length() < indent.length() -> indent
                    else -> it
                }
            }
            else -> indent + it
        }
    }
    .joinToString("\n")

private fun String.indentWidth(): Int = indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length() else it }

private fun getIndentFunction(indent: String) = when {
    indent.isEmpty() -> { line: String -> line }
    else -> { line: String -> "$indent$line" }
}

private inline fun List<String>.reindent(resultSizeEstimate: Int, indentAddFunction: (String) -> String, indentCutFunction: (String) -> String?) = lastIndex.let { lastLineIndex ->
    withIndex()
            .dropWhile { it.index == 0 && it.value.isBlank() }
            .takeWhile { it.index != lastLineIndex || it.value.isNotBlank() }
            .map { indentCutFunction(it.value)?.let { cutted -> indentAddFunction(cutted) } ?: it.value }
            .joinTo(StringBuilder(resultSizeEstimate), "\n")
            .toString()
}
