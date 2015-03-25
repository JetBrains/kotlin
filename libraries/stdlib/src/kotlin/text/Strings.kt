package kotlin

/** Returns the string with leading and trailing text matching the given string removed */
deprecated("Use removeEnclosing(text, text) or removePrefix(text).removeSuffix(text)")
public fun String.trim(text: String): String = removePrefix(text).removeSuffix(text)

/** Returns the string with the prefix and postfix text trimmed */
deprecated("Use removeEnclosing(prefix, suffix) or removePrefix(prefix).removeSuffix(suffix)")
public fun String.trim(prefix: String, postfix: String): String = removePrefix(prefix).removeSuffix(postfix)

/**
 * Returns the string with leading and trailing characters matching the [predicate] trimmed.
 */
inline public fun String.trim(predicate: (Char) -> Boolean): String {
    var startIndex = 0
    var endIndex = length() - 1
    var startFound = false

    while (startIndex <= endIndex) {
        val index = if (!startFound) startIndex else endIndex
        val match = predicate(this[index])

        if (!startFound) {
            if (!match)
                startFound = true
            else
                startIndex += 1
        }
        else {
            if (!match)
                break
            else
                endIndex -= 1
        }
    }

    return substring(startIndex, endIndex + 1)
}

/**
 * Returns the string with leading characters matching the [predicate] trimmed.
 */
inline public fun String.trimStart(predicate: (Char) -> Boolean): String {
    for (index in this.indices)
        if (!predicate(this[index]))
            return substring(index)

    return ""
}

/**
 * Returns the string with trailing characters matching the [predicate] trimmed.
 */
inline public fun String.trimEnd(predicate: (Char) -> Boolean): String {
    for (index in this.indices.reversed())
        if (!predicate(this[index]))
            return substring(0, index + 1)

    return ""
}

/**
 * Returns the string with leading and trailing characters in the [chars] array trimmed.
 */
public fun String.trim(vararg chars: Char): String = trim { it in chars }

/**
 * Returns the string with leading and trailing characters in the [chars] array trimmed.
 */
public fun String.trimStart(vararg chars: Char): String = trimStart { it in chars }

/**
 * Returns the string with trailing characters in the [chars] array trimmed.
 */
public fun String.trimEnd(vararg chars: Char): String = trimEnd { it in chars }

deprecated("Use removePrefix() instead")
public fun String.trimLeading(prefix: String): String = removePrefix(prefix)

deprecated("Use removeSuffix() instead")
public fun String.trimTrailing(postfix: String): String = removeSuffix(postfix)

/**
 * Returns a string with leading and trailing whitespace trimmed.
 */
public fun String.trim(): String = trim { it.isWhitespace() }

/**
 * Returns a string with leading whitespace removed.
 */
public fun String.trimStart(): String = trimStart { it.isWhitespace() }

deprecated("Use trimStart instead.")
public fun String.trimLeading(): String = trimStart { it.isWhitespace() }

/**
 * Returns a string with trailing whitespace removed.
 */
public fun String.trimEnd(): String = trimEnd { it.isWhitespace() }

deprecated("Use trimEnd instead.")
public fun String.trimTrailing(): String = trimEnd { it.isWhitespace() }

/**
 * Left pad a String with a specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @returns Returns a string, of length at least [length], consisting of string prepended with [padChar] as many times.
 * as are necessary to reach that length.
 */
public fun String.padStart(length: Int, padChar: Char = ' '): String {
    if (length < 0)
        throw IllegalArgumentException("String length $length is less than zero.")
    if (length <= this.length())
        return this

    val sb = StringBuilder(length)
    for (i in 1..(length - this.length()))
        sb.append(padChar)
    sb.append(this)
    return sb.toString()
}

/**
 * Right pad a String with a specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @returns Returns a string, of length at least [length], consisting of string prepended with [padChar] as many times.
 * as are necessary to reach that length.
 */
public fun String.padEnd(length: Int, padChar: Char = ' '): String {
    if (length < 0)
        throw IllegalArgumentException("String length $length is less than zero.")
    if (length <= this.length())
        return this

    val sb = StringBuilder(length)
    sb.append(this)
    for (i in 1..(length - this.length()))
        sb.append(padChar)
    return sb.toString()
}

/** Returns true if the string is not null and not empty */
public fun String?.isNotEmpty(): Boolean = this != null && this.length() > 0

/**
 * Iterator for characters of given CharSequence
 */
public fun CharSequence.iterator(): CharIterator = object : CharIterator() {
    private var index = 0

    public override fun nextChar(): Char = get(index++)

    public override fun hasNext(): Boolean = index < length
}

/** Returns the string if it is not null, or the empty string otherwise. */
public fun String?.orEmpty(): String = this ?: ""

/**
 * Returns the range of valid character indices for this string.
 */
public val String.indices: IntRange
    get() = 0..length() - 1

/**
 * Returns a character at the given index in a [CharSequence]. Allows to use the
 * index operator for working with character sequences:
 * ```
 * val c = charSequence[5]
 * ```
 */
public fun CharSequence.get(index: Int): Char = this.charAt(index)

/**
 * Returns the index of the last character in the String or -1 if the String is empty
 */
public val String.lastIndex: Int
    get() = this.length() - 1

/**
 * Returns a subsequence obtained by taking the characters at the given [indices] in this sequence.
 */
public fun CharSequence.slice(indices: Iterable<Int>): CharSequence {
    val sb = StringBuilder()
    for (i in indices) {
        sb.append(get(i))
    }
    return sb.toString()
}

/**
 * Returns a substring specified by the given [range].
 */
public fun String.substring(range: IntRange): String = substring(range.start, range.end + 1)

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun Iterable<String>.join(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 * If the array could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun Array<String>.join(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 * If the stream could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun Sequence<String>.join(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

deprecated("Migrate to using Sequence<T> and respective functions")
public fun Stream<String>.join(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

/**
 * Returns a substring before the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBefore(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring before the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBefore(delimiter: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring after the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfter(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + 1, length)
}

/**
 * Returns a substring after the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfter(delimiter: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + delimiter.length, length)
}

/**
 * Returns a substring before the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBeforeLast(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring before the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBeforeLast(delimiter: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring after the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfterLast(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + 1, length)
}

/**
 * Returns a substring after the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfterLast(delimiter: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + delimiter.length, length)
}

/**
 * Replaces the part of the string at the given range with the [replacement] string.
 * @param firstIndex the index of the first character to be replaced.
 * @param lastIndex the index of the first character after the replacement to keep in the string.
 */
public fun String.replaceRange(firstIndex: Int, lastIndex: Int, replacement: String): String {
    if (lastIndex < firstIndex)
        throw IndexOutOfBoundsException("Last index ($lastIndex) is less than first index ($firstIndex)")
    val sb = StringBuilder()
    sb.append(this, 0, firstIndex)
    sb.append(replacement)
    sb.append(this, lastIndex, length)
    return sb.toString()
}

/**
 * Replace the part of string at the given [range] with the [replacement] string.
 *
 * The end index of the [range] is included in the part to be replaced.
 */
public fun String.replaceRange(range: IntRange, replacement: String): String = replaceRange(range.start, range.end + 1, replacement)

/**
 * Removes the part of a string at a given range.
 * @param firstIndex the index of the first character to be removed.
 * @param lastIndex the index of the first character after the removed part to keep in the string.
*
*  [lastIndex] is not included in the removed part.
 */
public fun String.removeRange(firstIndex: Int, lastIndex: Int): String {
    if (lastIndex < firstIndex)
        throw IndexOutOfBoundsException("Last index ($lastIndex) is less than first index ($firstIndex)")

    if (lastIndex == firstIndex)
        return this

    val sb = StringBuilder(length() - (lastIndex - firstIndex))
    sb.append(this, 0, firstIndex)
    sb.append(this, lastIndex, length())
    return sb.toString()
}

/**
 * Removes the part of a string at the given [range].
 *
 * The end index of the [range] is included in the removed part.
 */
public fun String.removeRange(range: IntRange): String = removeRange(range.start, range.end + 1)

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
public fun String.removePrefix(prefix: String): String {
    if (startsWith(prefix)) {
        return substring(prefix.length())
    }
    return this
}

/**
 * If this string ends with the given [suffix], returns a copy of this string
 * with the suffix removed. Otherwise, returns this string.
 */
public fun String.removeSuffix(suffix: String): String {
    if (endsWith(suffix)) {
        return substring(0, length() - suffix.length())
    }
    return this
}

/**
 * Removes from a string both the given [prefix] and [suffix] if and only if
 * it starts with the [prefix] and ends with the [suffix].
 * Otherwise returns this string unchanged.
 */
public fun String.removeSurrounding(prefix: String, suffix: String): String {
    if (startsWith(prefix) && endsWith(suffix)) {
        return substring(prefix.length(), length() - suffix.length())
    }
    return this
}

/**
 * Removes the given [delimiter] string from both the start and the end of this string
 * if and only if it starts with and ends with the [delimiter].
 * Otherwise returns this string unchanged.
 */
public fun String.removeSurrounding(delimiter: String): String = removeSurrounding(delimiter, delimiter)

/**
 * Replace part of string before the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBefore(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string before the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBefore(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string after the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfter(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + 1, length, replacement)
}

/**
 * Replace part of string after the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfter(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + delimiter.length, length, replacement)
}

/**
 * Replace part of string after the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfterLast(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + delimiter.length, length, replacement)
}

/**
 * Replace part of string after the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfterLast(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + 1, length, replacement)
}

/**
 * Replace part of string before the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBeforeLast(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string before the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBeforeLast(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}
