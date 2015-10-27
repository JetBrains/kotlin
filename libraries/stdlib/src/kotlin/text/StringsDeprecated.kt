@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")


package kotlin

import java.util.NoSuchElementException
import kotlin.text.MatchResult
import kotlin.text.Regex

/**
 * Returns the string with leading and trailing characters matching the [predicate] trimmed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
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
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
inline public fun String.trimStart(predicate: (Char) -> Boolean): String {
    for (index in this.indices)
        if (!predicate(this[index]))
            return substring(index)

    return ""
}

/**
 * Returns the string with trailing characters matching the [predicate] trimmed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
inline public fun String.trimEnd(predicate: (Char) -> Boolean): String {
    for (index in this.indices.reversed())
        if (!predicate(this[index]))
            return substring(0, index + 1)

    return ""
}

/**
 * Returns the string with leading and trailing characters in the [chars] array trimmed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.trim(vararg chars: Char): String = trim { it in chars }

/**
 * Returns the string with leading and trailing characters in the [chars] array trimmed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.trimStart(vararg chars: Char): String = trimStart { it in chars }

/**
 * Returns the string with trailing characters in the [chars] array trimmed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.trimEnd(vararg chars: Char): String = trimEnd { it in chars }

/**
 * Returns a string with leading and trailing whitespace trimmed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.trim(): String = trim { it.isWhitespace() }

/**
 * Returns a string with leading whitespace removed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.trimStart(): String = trimStart { it.isWhitespace() }

/**
 * Returns a string with trailing whitespace removed.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.trimEnd(): String = trimEnd { it.isWhitespace() }

/**
 * Left pad a String with a specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @returns Returns a string, of length at least [length], consisting of string prepended with [padChar] as many times.
 * as are necessary to reach that length.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
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
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
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
*/

/**
 * Replaces the part of the string at the given range with the [replacement] string.
 * @param firstIndex the index of the first character to be replaced.
 * @param lastIndex the index of the first character after the replacement to keep in the string.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.replaceRange(firstIndex: Int, lastIndex: Int, replacement: String): String {
    if (lastIndex < firstIndex)
        throw IndexOutOfBoundsException("Last index ($lastIndex) is less than first index ($firstIndex)")
    val sb = StringBuilder()
    sb.append(this, 0, firstIndex)
    sb.append(replacement)
    sb.append(this, lastIndex, length())
    return sb.toString()
}

/**
 * Replace the part of string at the given [range] with the [replacement] string.
 *
 * The end index of the [range] is included in the part to be replaced.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.replaceRange(range: IntRange, replacement: String): String = replaceRange(range.start, range.end + 1, replacement)

/**
 * Removes the part of a string at a given range.
 * @param firstIndex the index of the first character to be removed.
 * @param lastIndex the index of the first character after the removed part to keep in the string.
 *
 *  [lastIndex] is not included in the removed part.
 */
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
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
@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun String.removeRange(range: IntRange): String = removeRange(range.start, range.end + 1)

/*

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

