package kotlin

/** Returns the string with leading and trailing text matching the given string removed */
public fun String.trim(text: String): String = trimLeading(text).trimTrailing(text)

/** Returns the string with the prefix and postfix text trimmed */
public fun String.trim(prefix: String, postfix: String): String = trimLeading(prefix).trimTrailing(postfix)

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
public fun String.trimLeading(prefix: String): String {
    var answer = this
    if (answer.startsWith(prefix)) {
        answer = answer.substring(prefix.length())
    }
    return answer
}

/**
 * If this string ends with the given [postfix], returns a copy of this string
 * with the postfix removed. Otherwise, returns this string.
 */
public fun String.trimTrailing(postfix: String): String {
    var answer = this
    if (answer.endsWith(postfix)) {
        answer = answer.substring(0, length() - postfix.length())
    }
    return answer
}

/**
 * Returns a copy of this String with leading whitespace removed.
 */
public fun String.trimLeading(): String {
    var count = 0

    while ((count < this.length) && (this[count] <= ' ')) {
        count++
    }
    return if (count > 0) substring(count) else this
}

/**
 * Returns a copy of this String with trailing whitespace removed.
 */
public fun String.trimTrailing(): String {
    var count = this.length

    while (count > 0 && this[count - 1] <= ' ') {
        count--
    }
    return if (count < this.length) substring(0, count) else this
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
 */
public fun String.replaceRange(range: IntRange, replacement: String): String {
    if (range.end < range.start)
        throw IndexOutOfBoundsException("Last index (${range.start}) is less than first index (${range.end})")
    val sb = StringBuilder()
    sb.append(this, 0, range.start)
    sb.append(replacement)
    sb.append(this, range.end, length)
    return sb.toString()
}

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
