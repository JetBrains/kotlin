@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")


package kotlin.text

import java.util.NoSuchElementException
import kotlin.text.MatchResult
import kotlin.text.Regex

/** Returns `true` if the string is not `null` and not empty */
@Deprecated("Use !isNullOrEmpty() or isNullOrEmpty().not() for nullable strings.", ReplaceWith("this != null && this.isNotEmpty()"), DeprecationLevel.ERROR)
@kotlin.jvm.JvmName("isNotEmptyNullable")
public fun CharSequence?.isNotEmpty(): Boolean = this != null && this.length() > 0


/** Returns `true` if the string is not `null` and not empty */
@Deprecated("Use !isNullOrEmpty() or isNullOrEmpty().not() for nullable strings.", ReplaceWith("this != null && this.isNotEmpty()"), DeprecationLevel.HIDDEN)
@kotlin.jvm.JvmName("isNotEmptyNullable")
public fun String?.isNotEmpty(): Boolean = this != null && this.length() > 0

/**
 * Returns `true` if this nullable string is either `null` or empty.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String?.isNullOrEmpty(): Boolean = this == null || this.length() == 0

/**
 * Returns `true` if this string is empty (contains no characters).
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.isEmpty(): Boolean = length() == 0

/**
 * Returns `true` if this string is not empty.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.isNotEmpty(): Boolean = length() > 0

// implemented differently in JVM and JS
//public fun String.isBlank(): Boolean = length() == 0 || all { it.isWhitespace() }


/**
 * Returns `true` if this string is not empty and contains some characters except of whitespace characters.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.isNotBlank(): Boolean = !isBlank()

/**
 * Returns `true` if this nullable string is either `null` or empty or consists solely of whitespace characters.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String?.isNullOrBlank(): Boolean = this == null || this.isBlank()

/**
 * Returns the range of valid character indices for this string.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public val String.indices: IntRange
    get() = 0..length() - 1

/**
 * Returns the index of the last character in the String or -1 if the String is empty.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public val String.lastIndex: Int
    get() = this.length() - 1

/*
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
    return if (index == -1) missingDelimiterValue else substring(index + 1, length())
}

/**
 * Returns a substring after the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfter(delimiter: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + delimiter.length(), length())
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
    return if (index == -1) missingDelimiterValue else substring(index + 1, length())
}

/**
 * Returns a substring after the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfterLast(delimiter: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + delimiter.length(), length())
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
    return if (index == -1) missingDelimiterValue else replaceRange(index + 1, length(), replacement)
}

/**
 * Replace part of string after the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfter(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + delimiter.length(), length(), replacement)
}

/**
 * Replace part of string after the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfterLast(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + delimiter.length(), length(), replacement)
}

/**
 * Replace part of string after the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfterLast(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + 1, length(), replacement)
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
*/


@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.replaceRange(firstIndex: Int, lastIndex: Int, replacement: String): String
        = replaceRange(firstIndex, lastIndex, replacement)

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.replaceRange(range: IntRange, replacement: String): String
        = replaceRange(range.start, range.end + 1, replacement)

private val IntRange.end: Int get() = endInclusive

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.removePrefix(prefix: String): String = removePrefix(prefix)

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.removeSuffix(suffix: String): String = removeSuffix(suffix)

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.removeSurrounding(prefix: String, suffix: String): String = removeSurrounding(prefix, suffix)

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.removeSurrounding(delimiter: String): String = removeSurrounding(delimiter, delimiter)



/**
 * Returns a new string obtained by replacing each substring of this string that matches the given regular expression
 * with the given [replacement].
 *
 * The [replacement] can consist of any combination of literal text and $-substitutions. To treat the replacement string
 * literally escape it with the [kotlin.text.Regex.Companion.escapeReplacement] method.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.replace(regex: Regex, replacement: String): String = regex.replace(this, replacement)

/**
 * Returns a new string obtained by replacing each substring of this string that matches the given regular expression
 * with the result of the given function [transform] that takes [MatchResult] and returns a string to be used as a
 * replacement for that match.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.replace(regex: Regex, transform: (MatchResult) -> String): String = regex.replace(this, transform)

/**
 * Replaces the first occurrence of the given regular expression [regex] in this string with specified [replacement] expression.
 *
 * @param replacement A replacement expression that can include substitutions. See [Regex.replaceFirst] for details.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.replaceFirst(regex: Regex, replacement: String): String = regex.replaceFirst(this, replacement)


/**
 * Returns `true` if this string matches the given regular expression.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.matches(regex: Regex): Boolean = regex.matches(this)


/**
 * Returns `true` if this string starts with the specified character.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.startsWith(char: Char, ignoreCase: Boolean = false): Boolean =
        this.length() > 0 && this[0].equals(char, ignoreCase)

/**
 * Returns `true` if this string ends with the specified character.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.endsWith(char: Char, ignoreCase: Boolean = false): Boolean =
        this.length() > 0 && this[lastIndex].equals(char, ignoreCase)





// indexOfAny()

private fun String.findAnyOf(chars: CharArray, startIndex: Int, ignoreCase: Boolean, last: Boolean): Pair<Int, Char>? {
    if (!ignoreCase && chars.size() == 1) {
        val char = chars.single()
        val index = if (!last) nativeIndexOf(char, startIndex) else nativeLastIndexOf(char, startIndex)
        return if (index < 0) null else index to char
    }

    val indices = if (!last) Math.max(startIndex, 0)..lastIndex else Math.min(startIndex, lastIndex) downTo 0
    for (index in indices) {
        val charAtIndex = get(index)
        val matchingCharIndex = chars.indexOfFirst { it.equals(charAtIndex, ignoreCase) }
        if (matchingCharIndex >= 0)
            return index to chars[matchingCharIndex]
    }

    return null
}

/**
 * Finds the index of the first occurrence of any of the specified [chars] in this string, starting from the specified [startIndex] and
 * optionally ignoring the case.
 *
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the first occurrence of matched character from [chars] or -1 if none of [chars] are found.
 *
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.indexOfAny(chars: CharArray, startIndex: Int = 0, ignoreCase: Boolean = false): Int =
        findAnyOf(chars, startIndex, ignoreCase, last = false)?.first ?: -1

/**
 * Finds the index of the last occurrence of any of the specified [chars] in this string, starting from the specified [startIndex] and
 * optionally ignoring the case.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the last occurrence of matched character from [chars] or -1 if none of [chars] are found.
 *
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.lastIndexOfAny(chars: CharArray, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int =
        findAnyOf(chars, startIndex, ignoreCase, last = true)?.first ?: -1


private fun String.findAnyOf(strings: Collection<String>, startIndex: Int, ignoreCase: Boolean, last: Boolean): Pair<Int, String>? {
    if (!ignoreCase && strings.size() == 1) {
        val string = strings.single()
        val index = if (!last) nativeIndexOf(string, startIndex) else nativeLastIndexOf(string, startIndex)
        return if (index < 0) null else index to string
    }

    val indices = if (!last) Math.max(startIndex, 0)..length() else Math.min(startIndex, lastIndex) downTo 0
    for (index in indices) {
        val matchingString = strings.firstOrNull { it.regionMatches(0, this, index, it.length(), ignoreCase) }
        if (matchingString != null)
            return index to matchingString
    }

    return null
}

/**
 * Finds the first occurrence of any of the specified [strings] in this string, starting from the specified [startIndex] and
 * optionally ignoring the case.
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns A pair of an index of the first occurrence of matched string from [strings] and the string matched or `null` if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.findAnyOf(strings: Collection<String>, startIndex: Int = 0, ignoreCase: Boolean = false): Pair<Int, String>? =
        findAnyOf(strings, startIndex, ignoreCase, last = false)

/**
 * Finds the last occurrence of any of the specified [strings] in this string, starting from the specified [startIndex] and
 * optionally ignoring the case.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns A pair of an index of the last occurrence of matched string from [strings] and the string matched or `null` if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the end toward the beginning of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.findLastAnyOf(strings: Collection<String>, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Pair<Int, String>? =
        findAnyOf(strings, startIndex, ignoreCase, last = true)

/**
 * Finds the index of the first occurrence of any of the specified [strings] in this string, starting from the specified [startIndex] and
 * optionally ignoring the case.
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the first occurrence of matched string from [strings] or -1 if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.indexOfAny(strings: Collection<String>, startIndex: Int = 0, ignoreCase: Boolean = false): Int =
        findAnyOf(strings, startIndex, ignoreCase, last = false)?.first ?: -1

/**
 * Finds the index of the last occurrence of any of the specified [strings] in this string, starting from the specified [startIndex] and
 * optionally ignoring the case.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the last occurrence of matched string from [strings] or -1 if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the end toward the beginning of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.lastIndexOfAny(strings: Collection<String>, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int =
        findAnyOf(strings, startIndex, ignoreCase, last = true)?.first ?: -1


// indexOf

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the first occurrence of [char] or -1 if none is found.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.indexOf(char: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
    return if (ignoreCase)
        indexOfAny(charArrayOf(char), startIndex, ignoreCase)
    else
        nativeIndexOf(char, startIndex)
}

/**
 * Returns the index within this string of the first occurrence of the specified [string], starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the first occurrence of [string] or -1 if none is found.
 */
@kotlin.jvm.JvmOverloads
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.indexOf(string: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
    return if (ignoreCase)
        indexOfAny(listOf(string), startIndex, ignoreCase)
    else
        nativeIndexOf(string, startIndex)
}

/**
 * Returns the index within this string of the last occurrence of the specified character, starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the first occurrence of [char] or -1 if none is found.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.lastIndexOf(char: Char, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int {
    return if (ignoreCase)
        lastIndexOfAny(charArrayOf(char), startIndex, ignoreCase)
    else
        nativeLastIndexOf(char, startIndex)
}


/**
 * Returns the index within this string of the last occurrence of the specified [string], starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the first occurrence of [string] or -1 if none is found.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.lastIndexOf(string: String, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int {
    return if (ignoreCase)
        lastIndexOfAny(listOf(string), startIndex, ignoreCase)
    else
        nativeLastIndexOf(string, startIndex)
}

/**
 * Returns `true` if this string contains the specified sequence of characters as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public operator fun String.contains(seq: CharSequence, ignoreCase: Boolean = false): Boolean =
        indexOf(seq.toString(), ignoreCase = ignoreCase) >= 0


/**
 * Returns `true` if this string contains the specified character.
 *
 * @param ignoreCase `true` to ignore character case when comparing characters. By default `false`.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public operator fun String.contains(char: Char, ignoreCase: Boolean = false): Boolean =
        indexOf(char, ignoreCase = ignoreCase) >= 0




// split

/**
 * Splits this string to a sequence of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.splitToSequence(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
        splitToSequence(*delimiters, ignoreCase = ignoreCase, limit = limit)

/**
 * Splits this string to a list of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and matches at each position the first element in [delimiters]
 * that is equal to a delimiter in this instance at that position.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.split(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String> =
        splitToSequence(*delimiters, ignoreCase = ignoreCase, limit = limit).toList()

/**
 * Splits this string to a sequence of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.splitToSequence(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
        splitToSequence(*delimiters, ignoreCase = ignoreCase, limit = limit)

/**
 * Splits this string to a list of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.split(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): List<String> =
        splitToSequence(*delimiters, ignoreCase = ignoreCase, limit = limit).toList()

/**
 * Splits this string around matches of the given regular expression.
 *
 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.split(pattern: Regex, limit: Int = 0): List<String> = pattern.split(this, limit)

/**
 * Splits this string to a sequence of lines delimited by any of the following character sequences: CRLF, LF or CR.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.lineSequence(): Sequence<String> = lineSequence()

/**
 * * Splits this string to a list of lines delimited by any of the following character sequences: CRLF, LF or CR.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.lines(): List<String> = lineSequence().toList()

