package kotlin

import java.util.NoSuchElementException
import kotlin.platform.platformName
import kotlin.text.MatchResult
import kotlin.text.Regex

/** Returns the string with leading and trailing text matching the given string removed */
deprecated("Use removeSurrounding(text, text) or removePrefix(text).removeSuffix(text)")
public fun String.trim(text: String): String = removePrefix(text).removeSuffix(text)

/** Returns the string with the prefix and postfix text trimmed */
deprecated("Use removeSurrounding(prefix, suffix) or removePrefix(prefix).removeSuffix(suffix)")
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

deprecated("Use removePrefix() instead", ReplaceWith("removePrefix(prefix)"))
public fun String.trimLeading(prefix: String): String = removePrefix(prefix)

deprecated("Use removeSuffix() instead", ReplaceWith("removeSuffix(postfix)"))
public fun String.trimTrailing(postfix: String): String = removeSuffix(postfix)

/**
 * Returns a string with leading and trailing whitespace trimmed.
 */
public fun String.trim(): String = trim { it.isWhitespace() }

/**
 * Returns a string with leading whitespace removed.
 */
public fun String.trimStart(): String = trimStart { it.isWhitespace() }

deprecated("Use trimStart instead.", ReplaceWith("trimStart()"))
public fun String.trimLeading(): String = trimStart { it.isWhitespace() }

/**
 * Returns a string with trailing whitespace removed.
 */
public fun String.trimEnd(): String = trimEnd { it.isWhitespace() }

deprecated("Use trimEnd instead.", ReplaceWith("trimEnd()"))
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

/** Returns `true` if the string is not `null` and not empty */
deprecated("Use !isNullOrEmpty() or isNullOrEmpty().not() for nullable strings.", ReplaceWith("this != null && this.isNotEmpty()"))
platformName("isNotEmptyNullable")
public fun String?.isNotEmpty(): Boolean = this != null && this.length() > 0

/**
 * Returns `true` if this nullable string is either `null` or empty.
 */
public fun String?.isNullOrEmpty(): Boolean = this == null || this.length() == 0

/**
 * Returns `true` if this string is empty (contains no characters).
 */
public fun String.isEmpty(): Boolean = length() == 0

/**
 * Returns `true` if this string is not empty.
 */
public fun String.isNotEmpty(): Boolean = length() > 0

// implemented differently in JVM and JS
//public fun String.isBlank(): Boolean = length() == 0 || all { it.isWhitespace() }


/**
 * Returns `true` if this string is not empty and contains some characters except of whitespace characters.
 */
public fun String.isNotBlank(): Boolean = !isBlank()

/**
 * Returns `true` if this nullable string is either `null` or empty or consists solely of whitespace characters.
 */
public fun String?.isNullOrBlank(): Boolean = this == null || this.isBlank()

/**
 * Iterator for characters of given CharSequence.
 */
public fun CharSequence.iterator(): CharIterator = object : CharIterator() {
    private var index = 0

    public override fun nextChar(): Char = get(index++)

    public override fun hasNext(): Boolean = index < length()
}

/** Returns the string if it is not `null`, or the empty string otherwise. */
public fun String?.orEmpty(): String = this ?: ""

/**
 * Returns the range of valid character indices for this string.
 */
public val String.indices: IntRange
    get() = 0..length() - 1

/**
 * Returns the index of the last character in the String or -1 if the String is empty.
 */
public val String.lastIndex: Int
    get() = this.length() - 1

/**
 * Returns a character at the given index in a [CharSequence]. Allows to use the
 * index operator for working with character sequences:
 * ```
 * val c = charSequence[5]
 * ```
 */
public fun CharSequence.get(index: Int): Char = this.charAt(index)

/**
 * Returns `true` if this CharSequence has Unicode surrogate pair at the specified [index].
 */
public fun CharSequence.hasSurrogatePairAt(index: Int): Boolean {
    return index in 0..length() - 2
            && this[index].isHighSurrogate()
            && this[index + 1].isLowSurrogate()
}

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
    sb.append(this, lastIndex, length())
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


// public fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean): String // JVM- and JS-specific
// public fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean): String // JVM- and JS-specific

/**
 * Returns a new string obtained by replacing each substring of this string that matches the given regular expression
 * with the given [replacement].
 *
 * The [replacement] can consist of any combination of literal text and $-substitutions. To treat the replacement string
 * literally escape it with the [kotlin.text.Regex.Companion.escapeReplacement] method.
 */
public fun String.replace(regex: Regex, replacement: String): String = regex.replace(this, replacement)

/**
 * Returns a new string obtained by replacing each substring of this string that matches the given regular expression
 * with the result of the given function [transform] that takes [MatchResult] and returns a string to be used as a
 * replacement for that match.
 */
public fun String.replace(regex: Regex, transform: (MatchResult) -> String): String = regex.replace(this, transform)

/**
 * Replaces the first occurrence of the given regular expression [regex] in this string with specified [replacement] expression.
 *
 * @param replacement A replacement expression that can include substitutions. See [Regex.replaceFirst] for details.
 */
public fun String.replaceFirst(regex: Regex, replacement: String): String = regex.replaceFirst(this, replacement)


/**
 * Returns `true` if this string matches the given regular expression.
 */
public fun String.matches(regex: Regex): Boolean = regex.matches(this)


/**
 * Returns `true` if this string starts with the specified character.
 */
public fun String.startsWith(char: Char, ignoreCase: Boolean = false): Boolean =
        this.length() > 0 && this[0].equals(char, ignoreCase)

/**
 * Returns `true` if this string ends with the specified character.
 */
public fun String.endsWith(char: Char, ignoreCase: Boolean = false): Boolean =
        this.length() > 0 && this[lastIndex].equals(char, ignoreCase)



// common prefix and suffix

/**
 * Returns the longest string `prefix` such that this string and [other] string both start with this prefix,
 * taking care not to split surrogate pairs.
 * If this and [other] have no common prefix, returns the empty string.

 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 */
public fun CharSequence.commonPrefixWith(other: CharSequence, ignoreCase: Boolean = false): String {
    val shortestLength = Math.min(this.length(), other.length())

    var i = 0
    while (i < shortestLength && this[i].equals(other[i], ignoreCase = ignoreCase)) {
        i++
    }
    if (this.hasSurrogatePairAt(i - 1) || other.hasSurrogatePairAt(i - 1)) {
        i--
    }
    return subSequence(0, i).toString()
}

/**
 * Returns the longest string `suffix` such that this string and [other] string both end with this suffix,
 * taking care not to split surrogate pairs.
 * If this and [other] have no common suffix, returns the empty string.

 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 */
public fun CharSequence.commonSuffixWith(other: CharSequence, ignoreCase: Boolean = false): String {
    val thisLength = this.length()
    val otherLength = other.length()
    val shortestLength = Math.min(thisLength, otherLength)

    var i = 0
    while (i < shortestLength && this[thisLength - i - 1].equals(other[otherLength - i - 1], ignoreCase = ignoreCase)) {
        i++
    }
    if (this.hasSurrogatePairAt(thisLength - i - 1) || other.hasSurrogatePairAt(otherLength - i - 1)) {
        i--;
    }
    return subSequence(thisLength - i, thisLength).toString();
}


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
public fun String.lastIndexOfAny(strings: Collection<String>, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int =
    findAnyOf(strings, startIndex, ignoreCase, last = true)?.first ?: -1


// indexOf

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the first occurrence of [char] or -1 if none is found.
 */
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
kotlin.jvm.jvmOverloads
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
public fun String.contains(seq: CharSequence, ignoreCase: Boolean = false): Boolean =
        indexOf(seq.toString(), ignoreCase = ignoreCase) >= 0


/**
 * Returns `true` if this string contains the specified character.
 *
 * @param ignoreCase `true` to ignore character case when comparing characters. By default `false`.
 */
public fun String.contains(char: Char, ignoreCase: Boolean = false): Boolean =
        indexOf(char, ignoreCase = ignoreCase) >= 0

/**
 * Trims leading whitespace characters followed by [marginPrefix] from every line of a [java.lang.CharSequence].
 * Always creates a new string even if the original CharSequence is mutable
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
public fun CharSequence.trimMargin(marginPrefix: String = "|", whitespacePredicate: (Char) -> Boolean = { it.isWhitespace() }): String =
        toString().lineSequence().map { line ->
            val content = line.trimStart(whitespacePredicate)

            when {
                content.startsWith(marginPrefix) -> content.removePrefix(marginPrefix)
                else -> line
            }
        }.joinTo(StringBuilder(length()), "\n").toString()

// rangesDelimitedBy


private class DelimitedRangesSequence(private val string: String, private val startIndex: Int, private val limit: Int, private val getNextMatch: String.(Int) -> Pair<Int, Int>?): Sequence<IntRange> {

    override fun iterator(): Iterator<IntRange> = object : Iterator<IntRange> {
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var currentStartIndex: Int = Math.min(Math.max(startIndex, 0), string.length())
        var nextSearchIndex: Int = currentStartIndex
        var nextItem: IntRange? = null
        var counter: Int = 0

        private fun calcNext() {
            if (nextSearchIndex < 0) {
                nextState = 0
                nextItem = null
            }
            else {
                if (limit > 0 && ++counter >= limit || nextSearchIndex > string.length()) {
                    nextItem = currentStartIndex..string.lastIndex
                    nextSearchIndex = -1
                }
                else {
                    val match = string.getNextMatch(nextSearchIndex)
                    if (match == null) {
                        nextItem = currentStartIndex..string.lastIndex
                        nextSearchIndex = -1
                    }
                    else {
                        val (index,length) = match
                        nextItem = currentStartIndex..index-1
                        currentStartIndex = index + length
                        nextSearchIndex = currentStartIndex + if (length == 0) 1 else 0
                    }
                }
                nextState = 1
            }
        }

        override fun next(): IntRange {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem as IntRange
            // Clean next to avoid keeping reference on yielded instance
            nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}

/**
 * Returns a sequence of index ranges of substrings in this string around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param startIndex The index to start searching delimiters from.
 *  No range having its start value less than [startIndex] is returned.
 *  [startIndex] is coerced to be non-negative and not greater than length of this string.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 */
private fun String.rangesDelimitedBy(vararg delimiters: Char, startIndex: Int = 0, ignoreCase: Boolean = false, limit: Int = 0): Sequence<IntRange> {
    require(limit >= 0, { "Limit must be non-negative, but was $limit" })

    return DelimitedRangesSequence(this, startIndex, limit, { startIndex -> findAnyOf(delimiters, startIndex, ignoreCase = ignoreCase, last = false)?.let { it.first to 1 } })
}


/**
 * Returns a sequence of index ranges of substrings in this string around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param startIndex The index to start searching delimiters from.
 *  No range having its start value less than [startIndex] is returned.
 *  [startIndex] is coerced to be non-negative and not greater than length of this string.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 */
private fun String.rangesDelimitedBy(vararg delimiters: String, startIndex: Int = 0, ignoreCase: Boolean = false, limit: Int = 0): Sequence<IntRange> {
    require(limit >= 0, { "Limit must be non-negative, but was $limit" } )
    val delimitersList = delimiters.asList()

    return DelimitedRangesSequence(this, startIndex, limit, { startIndex -> findAnyOf(delimitersList, startIndex, ignoreCase = ignoreCase, last = false)?.let { it.first to it.second.length ()} })

}


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
public fun String.splitToSequence(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
        rangesDelimitedBy(*delimiters, ignoreCase = ignoreCase, limit = limit) map { substring(it) }

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
public fun String.split(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String> =
        splitToSequence(*delimiters, ignoreCase = ignoreCase, limit = limit).toList()

deprecated("Use split(delimiters) instead.", ReplaceWith("split(*delimiters, ignoreCase = ignoreCase, limit = limit)"))
public fun String.splitBy(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String> =
        splitToSequence(*delimiters, ignoreCase = ignoreCase, limit = limit).toList()

/**
 * Splits this string to a sequence of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 */
public fun String.splitToSequence(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
        rangesDelimitedBy(*delimiters, ignoreCase = ignoreCase, limit = limit) map { substring(it) }

/**
 * Splits this string to a list of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 */
public fun String.split(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): List<String> =
        splitToSequence(*delimiters, ignoreCase = ignoreCase, limit = limit).toList()

/**
 * Splits this string around matches of the given regular expression.
 *
 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 */
public fun String.split(pattern: Regex, limit: Int = 0): List<String> = pattern.split(this, limit)

/**
 * Splits this string to a sequence of lines delimited by any of the following character sequences: CRLF, LF or CR.
 */
public fun String.lineSequence(): Sequence<String> = splitToSequence("\r\n", "\n", "\r")

/**
 * * Splits this string to a list of lines delimited by any of the following character sequences: CRLF, LF or CR.
 */
public fun String.lines(): List<String> = lineSequence().toList()
