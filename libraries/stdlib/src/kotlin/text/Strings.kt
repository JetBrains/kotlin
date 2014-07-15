package kotlin

/** Returns the string with leading and trailing text matching the given string removed */
public fun String.trim(text: String): String = trimLeading(text).trimTrailing(text)

/** Returns the string with the prefix and postfix text trimmed */
public fun String.trim(prefix: String, postfix: String): String = trimLeading(prefix).trimTrailing(postfix)

/** Returns the string with the leading prefix of this string removed */
public fun String.trimLeading(prefix: String): String {
    var answer = this
    if (answer.startsWith(prefix)) {
        answer = answer.substring(prefix.length())
    }
    return answer
}

/** Returns the string with the trailing postfix of this string removed */
public fun String.trimTrailing(postfix: String): String {
    var answer = this
    if (answer.endsWith(postfix)) {
        answer = answer.substring(0, length() - postfix.length())
    }
    return answer
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

/** Returns the string if it is not null or the empty string if its null */
public fun String?.orEmpty(): String = this ?: ""


// "Extension functions" for CharSequence

public val CharSequence.size: Int
    get() = this.length

public val String.size: Int
    get() = length()

public val String.indices: IntRange
    get() = 0..length() - 1

/**
 * Returns a subsequence specified by given set of indices.
 */
public fun CharSequence.slice(indices: Iterable<Int>): CharSequence {
    val sb = StringBuilder()
    for (i in indices) {
        sb.append(get(i))
    }
    return sb.toString()
}

/**
 * Returns a substring specified by given range
 */
public fun String.substring(range: IntRange): String = substring(range.start, range.end + 1)

/**
 * Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.
 * If a collection could be huge you can specify a non-negative value of *limit* which will only show a subset of the collection then it will
 * a special *truncated* separator (which defaults to "...")
 */
public fun Iterable<String>.join(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

/**
 * Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.
 * If an array could be huge you can specify a non-negative value of *limit* which will only show a subset of the array then it will
 * a special *truncated* separator (which defaults to "...")
 */
public fun Array<String>.join(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}

/**
 * Creates a string from all the elements separated using the *separator* and using the given *prefix* and *postfix* if supplied.
 * If a stream could be huge you can specify a non-negative value of *limit* which will only show a subset of the stream then it will
 * a special *truncated* separator (which defaults to "...")
 */
public fun Stream<String>.join(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "..."): String {
    return joinToString(separator, prefix, postfix, limit, truncated)
}
/**
 * Returns a substring before first occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringBefore(delimiter: Char, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(0, index)
}

/**
 * Returns a substring before first occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringBefore(delimiter: String, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(0, index)
}
/**
 * Returns a substring after first occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringAfter(delimiter: Char, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(index + 1, length)
}

/**
 * Returns a substring after first occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringAfter(delimiter: String, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(index + delimiter.length, length)
}

/**
 * Returns a substring before last occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringBeforeLast(delimiter: Char, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(0, index)
}

/**
 * Returns a substring before last occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringBeforeLast(delimiter: String, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(0, index)
}

/**
 * Returns a substring after last occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringAfterLast(delimiter: Char, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(index + 1, length)
}

/**
 * Returns a substring after last occurrence of delimiter.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.substringAfterLast(delimiter: String, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else substring(index + delimiter.length, length)
}

/**
 * Replace part of string at given range with replacement string
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
 * Replace part of string at given range with replacement string
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
 * Replace part of string before first occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceBefore(delimiter: Char, replacement: String, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string before first occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceBefore(delimiter: String, replacement: String, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string after first occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceAfter(delimiter: Char, replacement: String, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(index + 1, length, replacement)
}

/**
 * Replace part of string after first occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceAfter(delimiter: String, replacement: String, missingSeparatorValue : String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(index + delimiter.length, length, replacement)
}

/**
 * Replace part of string after last occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceAfterLast(delimiter: String, replacement: String, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(index + delimiter.length, length, replacement)
}

/**
 * Replace part of string after last occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceAfterLast(delimiter: Char, replacement: String, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(index + 1, length, replacement)
}

/**
 * Replace part of string before last occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceBeforeLast(delimiter: Char, replacement: String, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string before last occurrence of given delimiter with replacement string.
 * In case of no delimiter, returns the value of missingSeparatorValue which defaults to original string.
 */
public fun String.replaceBeforeLast(delimiter: String, replacement: String, missingSeparatorValue : String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingSeparatorValue else replaceRange(0, index, replacement)
}
