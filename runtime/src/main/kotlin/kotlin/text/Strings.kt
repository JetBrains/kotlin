/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.text

import kotlin.comparisons.*

/**
 * Returns a sub sequence of this char sequence having leading and trailing characters matching the [predicate] trimmed.
 */
public inline fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence {
    var startIndex = 0
    var endIndex = length - 1
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

    return subSequence(startIndex, endIndex + 1)
}

/**
 * Returns a string with leading and trailing characters matching the [predicate] trimmed.
 */
public inline fun String.trim(predicate: (Char) -> Boolean): String
        = (this as CharSequence).trim(predicate).toString()

/**
 * Returns a sub sequence of this char sequence having leading characters matching the [predicate] trimmed.
 */
public inline fun CharSequence.trimStart(predicate: (Char) -> Boolean): CharSequence {
    for (index in this.indices)
        if (!predicate(this[index]))
            return subSequence(index, length)

    return ""
}

/**
 * Returns a string with leading characters matching the [predicate] trimmed.
 */
public inline fun String.trimStart(predicate: (Char) -> Boolean): String
        = (this as CharSequence).trimStart(predicate).toString()

/**
 * Returns a sub sequence of this char sequence having trailing characters matching the [predicate] trimmed.
 */
public inline fun CharSequence.trimEnd(predicate: (Char) -> Boolean): CharSequence {
    for (index in this.indices.reversed())
        if (!predicate(this[index]))
            return substring(0, index + 1)

    return ""
}

/**
 * Returns a string with trailing characters matching the [predicate] trimmed.
 */
public inline fun String.trimEnd(predicate: (Char) -> Boolean): String
        = (this as CharSequence).trimEnd(predicate).toString()

/**
 * Returns a sub sequence of this char sequence having leading and trailing characters from the [chars] array trimmed.
 */
public fun CharSequence.trim(vararg chars: Char): CharSequence = trim { it in chars }

/**
 * Returns a string with leading and trailing characters from the [chars] array trimmed.
 */
public fun String.trim(vararg chars: Char): String = trim { it in chars }

/**
 * Returns a sub sequence of this char sequence having leading and trailing characters from the [chars] array trimmed.
 */
public fun CharSequence.trimStart(vararg chars: Char): CharSequence = trimStart { it in chars }

/**
 * Returns a string with leading and trailing characters from the [chars] array trimmed.
 */
public fun String.trimStart(vararg chars: Char): String = trimStart { it in chars }

/**
 * Returns a sub sequence of this char sequence having trailing characters from the [chars] array trimmed.
 */
public fun CharSequence.trimEnd(vararg chars: Char): CharSequence = trimEnd { it in chars }

/**
 * Returns a string with trailing characters from the [chars] array trimmed.
 */
public fun String.trimEnd(vararg chars: Char): String = trimEnd { it in chars }

/**
 * Returns a sub sequence of this char sequence having leading and trailing whitespace trimmed.
 */
public fun CharSequence.trim(): CharSequence = trim { it.isWhitespace() }

/**
 * Returns a string with leading and trailing whitespace trimmed.
 */
@kotlin.internal.InlineOnly
public inline fun String.trim(): String = (this as CharSequence).trim().toString()

/**
 * Returns a sub sequence of this char sequence having leading whitespace removed.
 */
public fun CharSequence.trimStart(): CharSequence = trimStart { it.isWhitespace() }

/**
 * Returns a string with leading whitespace removed.
 */
@kotlin.internal.InlineOnly
public inline fun String.trimStart(): String = (this as CharSequence).trimStart().toString()

/**
 * Returns a sub sequence of this char sequence having trailing whitespace removed.
 */
public fun CharSequence.trimEnd(): CharSequence = trimEnd { it.isWhitespace() }

/**
 * Returns a string with trailing whitespace removed.
 */
@kotlin.internal.InlineOnly
public inline fun String.trimEnd(): String = (this as CharSequence).trimEnd().toString()

/**
 * Returns a char sequence with content of this char sequence padded at the beginning
 * to the specified [length] with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @returns Returns a string, of length at least [length], consisting of string prepended with [padChar] as many times.
 * as are necessary to reach that length.
 */
public fun CharSequence.padStart(length: Int, padChar: Char = ' '): CharSequence {
    if (length < 0)
        throw IllegalArgumentException("Desired length $length is less than zero.")
    if (length <= this.length)
        return this.subSequence(0, this.length)

    val sb = StringBuilder(length)
    for (i in 1..(length - this.length))
        sb.append(padChar)
    sb.append(this)
    return sb
}

/**
 * Pads the string to the specified [length] at the beginning with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @returns Returns a string, of length at least [length], consisting of string prepended with [padChar] as many times.
 * as are necessary to reach that length.
 */
public fun String.padStart(length: Int, padChar: Char = ' '): String
        = (this as CharSequence).padStart(length, padChar).toString()

/**
 * Returns a char sequence with content of this char sequence padded at the end
 * to the specified [length] with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @returns Returns a string, of length at least [length], consisting of string prepended with [padChar] as many times.
 * as are necessary to reach that length.
 */
public fun CharSequence.padEnd(length: Int, padChar: Char = ' '): CharSequence {
    if (length < 0)
        throw IllegalArgumentException("Desired length $length is less than zero.")
    if (length <= this.length)
        return this.subSequence(0, this.length)

    val sb = StringBuilder(length)
    sb.append(this)
    for (i in 1..(length - this.length))
        sb.append(padChar)
    return sb
}

/**
 * Pads the string to the specified [length] at the end with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @returns Returns a string, of length at least [length], consisting of string prepended with [padChar] as many times.
 * as are necessary to reach that length.
 */
public fun String.padEnd(length: Int, padChar: Char = ' '): String
        = (this as CharSequence).padEnd(length, padChar).toString()

/**
 * Returns `true` if this nullable char sequence is either `null` or empty.
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence?.isNullOrEmpty(): Boolean = this == null || this.length == 0

/**
 * Returns `true` if this char sequence is empty (contains no characters).
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.isEmpty(): Boolean = length == 0

/**
 * Returns `true` if this char sequence is not empty.
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.isNotEmpty(): Boolean = length > 0

// implemented differently in JVM and JS
//public fun String.isBlank(): Boolean = length() == 0 || all { it.isWhitespace() }


/**
 * Returns `true` if this char sequence is not empty and contains some characters except of whitespace characters.
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.isNotBlank(): Boolean = !isBlank()

/**
 * Returns `true` if this nullable char sequence is either `null` or empty or consists solely of whitespace characters.
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence?.isNullOrBlank(): Boolean = this == null || this.isBlank()

/**
 * Iterator for characters of the given char sequence.
 */
public operator fun CharSequence.iterator(): CharIterator = object : CharIterator() {
    private var index = 0

    public override fun nextChar(): Char = get(index++)

    public override fun hasNext(): Boolean = index < length
}

/** Returns the string if it is not `null`, or the empty string otherwise. */
@kotlin.internal.InlineOnly
public inline fun String?.orEmpty(): String = this ?: ""

/**
 * Returns the range of valid character indices for this char sequence.
 */
public val CharSequence.indices: IntRange
    get() = 0..length - 1

/**
 * Returns the index of the last character in the char sequence or -1 if it is empty.
 */
public val CharSequence.lastIndex: Int
    get() = this.length - 1

/**
 * Returns `true` if this CharSequence has Unicode surrogate pair at the specified [index].
 */
public fun CharSequence.hasSurrogatePairAt(index: Int): Boolean {
    return index in 0..length - 2
            && this[index].isHighSurrogate()
            && this[index + 1].isLowSurrogate()
}

/**
 * Returns a substring specified by the given [range] of indices.
 */
public fun String.substring(range: IntRange): String = substring(range.start, range.endInclusive + 1)

/**
 * Returns a subsequence of this char sequence specified by the given [range] of indices.
 */
public fun CharSequence.subSequence(range: IntRange): CharSequence = subSequence(range.start, range.endInclusive + 1)

/**
 * Returns a subsequence of this char sequence.
 *
 * This extension is chosen only for invocation with old-named parameters.
 * Replace parameter names with the same as those of [CharSequence.subSequence].
 */
@kotlin.internal.InlineOnly
@Deprecated("Use parameters named startIndex and endIndex.", ReplaceWith("subSequence(startIndex = start, endIndex = end)"))
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline fun String.subSequence(start: Int, end: Int): CharSequence = subSequence(start, end)

/**
 * Returns a substring of chars from a range of this char sequence starting at the [startIndex] and ending right before the [endIndex].
 *
 * @param startIndex the start index (inclusive).
 * @param endIndex the end index (exclusive). If not specified, the length of the char sequence is used.
 */
// TODO: uncomment as soon as inlining works for stdlib.
@Fixme
@kotlin.internal.InlineOnly
public /*inline*/ fun CharSequence.substring(startIndex: Int, endIndex: Int = length): String = subSequence(startIndex, endIndex).toString()

/**
 * Returns a substring of chars at indices from the specified [range] of this char sequence.
 */
public fun CharSequence.substring(range: IntRange): String = subSequence(range.start, range.endInclusive + 1).toString()

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
 * Returns a char sequence with content of this char sequence where its part at the given range
 * is replaced with the [replacement] char sequence.
 * @param startIndex the index of the first character to be replaced.
 * @param endIndex the index of the first character after the replacement to keep in the string.
 */
public fun CharSequence.replaceRange(startIndex: Int, endIndex: Int, replacement: CharSequence): CharSequence {
    if (endIndex < startIndex)
        throw IndexOutOfBoundsException("End index ($endIndex) is less than start index ($startIndex).")
    val sb = StringBuilder()
    sb.append(this, 0, startIndex)
    sb.append(replacement)
    sb.append(this, endIndex, length)
    return sb
}

/**
 * Replaces the part of the string at the given range with the [replacement] char sequence.
 * @param startIndex the index of the first character to be replaced.
 * @param endIndex the index of the first character after the replacement to keep in the string.
 */
@kotlin.internal.InlineOnly
public inline fun String.replaceRange(startIndex: Int, endIndex: Int, replacement: CharSequence): String
        = (this as CharSequence).replaceRange(startIndex, endIndex, replacement).toString()

/**
 * Returns a char sequence with content of this char sequence where its part at the given [range]
 * is replaced with the [replacement] char sequence.
 *
 * The end index of the [range] is included in the part to be replaced.
 */
public fun CharSequence.replaceRange(range: IntRange, replacement: CharSequence): CharSequence
        = replaceRange(range.start, range.endInclusive + 1, replacement)

/**
 * Replace the part of string at the given [range] with the [replacement] string.
 *
 * The end index of the [range] is included in the part to be replaced.
 */
@kotlin.internal.InlineOnly
public inline fun String.replaceRange(range: IntRange, replacement: CharSequence): String
        = (this as CharSequence).replaceRange(range, replacement).toString()

/**
 * Returns a char sequence with content of this char sequence where its part at the given range is removed.
 *
 * @param startIndex the index of the first character to be removed.
 * @param endIndex the index of the first character after the removed part to keep in the string.
 *
 * [endIndex] is not included in the removed part.
 */
public fun CharSequence.removeRange(startIndex: Int, endIndex: Int): CharSequence {
    if (endIndex < startIndex)
        throw IndexOutOfBoundsException("End index ($endIndex) is less than start index ($startIndex).")

    if (endIndex == startIndex)
        return this.subSequence(0, length)

    val sb = StringBuilder(length - (endIndex - startIndex))
    sb.append(this, 0, startIndex)
    sb.append(this, endIndex, length)
    return sb
}

/**
 * Removes the part of a string at a given range.
 * @param startIndex the index of the first character to be removed.
 * @param endIndex the index of the first character after the removed part to keep in the string.
 *
 *  [endIndex] is not included in the removed part.
 */
@kotlin.internal.InlineOnly
public inline fun String.removeRange(startIndex: Int, endIndex: Int): String
        = (this as CharSequence).removeRange(startIndex, endIndex).toString()

/**
 * Returns a char sequence with content of this char sequence where its part at the given [range] is removed.
 *
 * The end index of the [range] is included in the removed part.
 */
public fun CharSequence.removeRange(range: IntRange): CharSequence = removeRange(range.start, range.endInclusive + 1)

/**
 * Removes the part of a string at the given [range].
 *
 * The end index of the [range] is included in the removed part.
 */
@kotlin.internal.InlineOnly
public inline fun String.removeRange(range: IntRange): String
        = (this as CharSequence).removeRange(range).toString()

/**
 * If this char sequence starts with the given [prefix], returns a new char sequence
 * with the prefix removed. Otherwise, returns a new char sequence with the same characters.
 */
public fun CharSequence.removePrefix(prefix: CharSequence): CharSequence {
    if (startsWith(prefix)) {
        return subSequence(prefix.length, length)
    }
    return subSequence(0, length)
}

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
public fun String.removePrefix(prefix: CharSequence): String {
    if (startsWith(prefix)) {
        return substring(prefix.length)
    }
    return this
}

/**
 * If this char sequence ends with the given [suffix], returns a new char sequence
 * with the suffix removed. Otherwise, returns a new char sequence with the same characters.
 */
public fun CharSequence.removeSuffix(suffix: CharSequence): CharSequence {
    if (endsWith(suffix)) {
        return subSequence(0, length - suffix.length)
    }
    return subSequence(0, length)
}

/**
 * If this string ends with the given [suffix], returns a copy of this string
 * with the suffix removed. Otherwise, returns this string.
 */
public fun String.removeSuffix(suffix: CharSequence): String {
    if (endsWith(suffix)) {
        return substring(0, length - suffix.length)
    }
    return this
}

/**
 * When this char sequence starts with the given [prefix] and ends with the given [suffix],
 * returns a new char sequence having both the given [prefix] and [suffix] removed.
 * Otherwise returns a new char sequence with the same characters.
 */
public fun CharSequence.removeSurrounding(prefix: CharSequence, suffix: CharSequence): CharSequence {
    if ((length >= prefix.length + suffix.length) && startsWith(prefix) && endsWith(suffix)) {
        return subSequence(prefix.length, length - suffix.length)
    }
    return subSequence(0, length)
}

/**
 * Removes from a string both the given [prefix] and [suffix] if and only if
 * it starts with the [prefix] and ends with the [suffix].
 * Otherwise returns this string unchanged.
 */
public fun String.removeSurrounding(prefix: CharSequence, suffix: CharSequence): String {
    if ((length >= prefix.length + suffix.length) && startsWith(prefix) && endsWith(suffix)) {
        return substring(prefix.length, length - suffix.length)
    }
    return this
}

/**
 * When this char sequence starts with and ends with the given [delimiter],
 * returns a new char sequence having this [delimiter] removed both from the start and end.
 * Otherwise returns a new char sequence with the same characters.
 */
public fun CharSequence.removeSurrounding(delimiter: CharSequence): CharSequence = removeSurrounding(delimiter, delimiter)

/**
 * Removes the given [delimiter] string from both the start and the end of this string
 * if and only if it starts with and ends with the [delimiter].
 * Otherwise returns this string unchanged.
 */
public fun String.removeSurrounding(delimiter: CharSequence): String = removeSurrounding(delimiter, delimiter)

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


// public fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean): String // JVM- and JS-specific
// public fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean): String // JVM- and JS-specific

/**
 * Returns a new string obtained by replacing each substring of this char sequence that matches the given regular expression
 * with the given [replacement].
 *
 * The [replacement] can consist of any combination of literal text and $-substitutions. To treat the replacement string
 * literally escape it with the [kotlin.text.Regex.Companion.escapeReplacement] method.
 */
//@FixmeRegex
//@kotlin.internal.InlineOnly
//public inline fun CharSequence.replace(regex: Regex, replacement: String): String = regex.replace(this, replacement)

/**
 * Returns a new string obtained by replacing each substring of this char sequence that matches the given regular expression
 * with the result of the given function [transform] that takes [MatchResult] and returns a string to be used as a
 * replacement for that match.
 */
//@FixmeRegex
//@kotlin.internal.InlineOnly
//public inline fun CharSequence.replace(regex: Regex, noinline transform: (MatchResult) -> CharSequence): String = regex.replace(this, transform)

/**
 * Replaces the first occurrence of the given regular expression [regex] in this char sequence with specified [replacement] expression.
 *
 * @param replacement A replacement expression that can include substitutions. See [Regex.replaceFirst] for details.
 */
//@FixmeRegex
//@kotlin.internal.InlineOnly
//public inline fun CharSequence.replaceFirst(regex: Regex, replacement: String): String = regex.replaceFirst(this, replacement)


/**
 * Returns `true` if this char sequence matches the given regular expression.
 */
//@FixmeRegex
//@kotlin.internal.InlineOnly
//public inline fun CharSequence.matches(regex: Regex): Boolean = regex.matches(this)

/**
 * Implementation of [regionMatches] for CharSequences.
 * Invoked when it's already known that arguments are not Strings, so that no additional type checks are performed.
 */
internal fun CharSequence.regionMatchesImpl(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean): Boolean {
    if ((otherOffset < 0) || (thisOffset < 0) || (thisOffset > this.length - length)
            || (otherOffset > other.length - length)) {
        return false
    }

    for (index in 0..length-1) {
        if (!this[thisOffset + index].equals(other[otherOffset + index], ignoreCase))
            return false
    }
    return true
}

/**
 * Returns `true` if this char sequence starts with the specified character.
 */
public fun CharSequence.startsWith(char: Char, ignoreCase: Boolean = false): Boolean =
        this.length > 0 && this[0].equals(char, ignoreCase)

/**
 * Returns `true` if this char sequence ends with the specified character.
 */
public fun CharSequence.endsWith(char: Char, ignoreCase: Boolean = false): Boolean =
        this.length > 0 && this[lastIndex].equals(char, ignoreCase)

/**
 * Returns `true` if this char sequence starts with the specified prefix.
 */
public fun CharSequence.startsWith(prefix: CharSequence, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase && this is String && prefix is String)
        return this.startsWith(prefix)
    else
        return regionMatchesImpl(0, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if a substring of this char sequence starting at the specified offset [startIndex] starts with the specified prefix.
 */
public fun CharSequence.startsWith(prefix: CharSequence, startIndex: Int, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase && this is String && prefix is String)
        return this.startsWith(prefix, startIndex)
    else
        return regionMatchesImpl(startIndex, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if this char sequence ends with the specified suffix.
 */
public fun CharSequence.endsWith(suffix: CharSequence, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase && this is String && suffix is String)
        return this.endsWith(suffix)
    else
        return regionMatchesImpl(length - suffix.length, suffix, 0, suffix.length, ignoreCase)
}


// common prefix and suffix

/**
 * Returns the longest string `prefix` such that this char sequence and [other] char sequence both start with this prefix,
 * taking care not to split surrogate pairs.
 * If this and [other] have no common prefix, returns the empty string.

 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 */
public fun CharSequence.commonPrefixWith(other: CharSequence, ignoreCase: Boolean = false): String {
    val shortestLength = minOf(this.length, other.length)

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
 * Returns the longest string `suffix` such that this char sequence and [other] char sequence both end with this suffix,
 * taking care not to split surrogate pairs.
 * If this and [other] have no common suffix, returns the empty string.

 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 */
public fun CharSequence.commonSuffixWith(other: CharSequence, ignoreCase: Boolean = false): String {
    val thisLength = this.length
    val otherLength = other.length
    val shortestLength = minOf(thisLength, otherLength)

    var i = 0
    while (i < shortestLength && this[thisLength - i - 1].equals(other[otherLength - i - 1], ignoreCase = ignoreCase)) {
        i++
    }
    if (this.hasSurrogatePairAt(thisLength - i - 1) || other.hasSurrogatePairAt(otherLength - i - 1)) {
        i--
    }
    return subSequence(thisLength - i, thisLength).toString()
}


// indexOfAny()

private fun CharSequence.findAnyOf(chars: CharArray, startIndex: Int, ignoreCase: Boolean, last: Boolean): Pair<Int, Char>? {
    if (!ignoreCase && chars.size == 1 && this is String) {
        val char = chars.single()
        val index = if (!last) nativeIndexOf(char, startIndex) else nativeLastIndexOf(char, startIndex)
        return if (index < 0) null else index to char
    }

    val indices = if (!last) startIndex.coerceAtLeast(0)..lastIndex else startIndex.coerceAtMost(lastIndex) downTo 0
    for (index in indices) {
        val charAtIndex = get(index)
        val matchingCharIndex = chars.indexOfFirst { it.equals(charAtIndex, ignoreCase) }
        if (matchingCharIndex >= 0)
            return index to chars[matchingCharIndex]
    }

    return null
}

/**
 * Finds the index of the first occurrence of any of the specified [chars] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the first occurrence of matched character from [chars] or -1 if none of [chars] are found.
 *
 */
public fun CharSequence.indexOfAny(chars: CharArray, startIndex: Int = 0, ignoreCase: Boolean = false): Int =
        findAnyOf(chars, startIndex, ignoreCase, last = false)?.first ?: -1

/**
 * Finds the index of the last occurrence of any of the specified [chars] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the last occurrence of matched character from [chars] or -1 if none of [chars] are found.
 *
 */
public fun CharSequence.lastIndexOfAny(chars: CharArray, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int =
        findAnyOf(chars, startIndex, ignoreCase, last = true)?.first ?: -1


private fun CharSequence.indexOf(other: CharSequence, startIndex: Int, endIndex: Int, ignoreCase: Boolean, last: Boolean = false): Int {
    val indices = if (!last)
        startIndex.coerceAtLeast(0)..endIndex.coerceAtMost(length)
    else
        startIndex.coerceAtMost(lastIndex) downTo endIndex.coerceAtLeast(0)

    if (this is String && other is String) { // smart cast
        for (index in indices) {
            if (other.regionMatches(0, this, index, other.length, ignoreCase))
                return index
        }
    } else {
        for (index in indices) {
            if (other.regionMatchesImpl(0, this, index, other.length, ignoreCase))
                return index
        }
    }
    return -1
}

private fun CharSequence.findAnyOf(strings: Collection<String>, startIndex: Int, ignoreCase: Boolean, last: Boolean): Pair<Int, String>? {
    if (!ignoreCase && strings.size == 1) {
        val string = strings.single()
        val index = if (!last) indexOf(string, startIndex) else lastIndexOf(string, startIndex)
        return if (index < 0) null else index to string
    }

    val indices = if (!last) startIndex.coerceAtLeast(0)..length else startIndex.coerceAtMost(lastIndex) downTo 0

    if (this is String) {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatches(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    } else {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatchesImpl(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    }

    return null
}

/**
 * Finds the first occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns A pair of an index of the first occurrence of matched string from [strings] and the string matched
 * or `null` if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
public fun CharSequence.findAnyOf(strings: Collection<String>, startIndex: Int = 0, ignoreCase: Boolean = false): Pair<Int, String>? =
        findAnyOf(strings, startIndex, ignoreCase, last = false)

/**
 * Finds the last occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns A pair of an index of the last occurrence of matched string from [strings] and the string matched or `null` if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the end toward the beginning of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
public fun CharSequence.findLastAnyOf(strings: Collection<String>, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Pair<Int, String>? =
        findAnyOf(strings, startIndex, ignoreCase, last = true)

/**
 * Finds the index of the first occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the first occurrence of matched string from [strings] or -1 if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
public fun CharSequence.indexOfAny(strings: Collection<String>, startIndex: Int = 0, ignoreCase: Boolean = false): Int =
        findAnyOf(strings, startIndex, ignoreCase, last = false)?.first ?: -1

/**
 * Finds the index of the last occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the last occurrence of matched string from [strings] or -1 if none of [strings] are found.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the end toward the beginning of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 */
public fun CharSequence.lastIndexOfAny(strings: Collection<String>, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int =
        findAnyOf(strings, startIndex, ignoreCase, last = true)?.first ?: -1


// indexOf

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the first occurrence of [char] or -1 if none is found.
 */
public fun CharSequence.indexOf(char: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        indexOfAny(charArrayOf(char), startIndex, ignoreCase)
    else
        nativeIndexOf(char, startIndex)
}

/**
 * Returns the index within this char sequence of the first occurrence of the specified [string],
 * starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the first occurrence of [string] or `-1` if none is found.
 */
public fun CharSequence.indexOf(string: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        indexOf(string, startIndex, length, ignoreCase)
    else
        nativeIndexOf(string, startIndex)
}

/**
 * Returns the index within this char sequence of the last occurrence of the specified character,
 * starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @returns An index of the first occurrence of [char] or -1 if none is found.
 */
public fun CharSequence.lastIndexOf(char: Char, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        lastIndexOfAny(charArrayOf(char), startIndex, ignoreCase)
    else
        nativeLastIndexOf(char, startIndex)
}

/**
 * Returns the index within this char sequence of the last occurrence of the specified [string],
 * starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @returns An index of the first occurrence of [string] or -1 if none is found.
 */
public fun CharSequence.lastIndexOf(string: String, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        indexOf(string, startIndex, 0, ignoreCase, last = true)
    else
        nativeLastIndexOf(string, startIndex)
}

/**
 * Returns `true` if this char sequence contains the specified [other] sequence of characters as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
public operator fun CharSequence.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean =
        if (other is String)
            indexOf(other, ignoreCase = ignoreCase) >= 0
        else
            indexOf(other, 0, length, ignoreCase) >= 0



/**
 * Returns `true` if this char sequence contains the specified character [char].
 *
 * @param ignoreCase `true` to ignore character case when comparing characters. By default `false`.
 */
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
public operator fun CharSequence.contains(char: Char, ignoreCase: Boolean = false): Boolean =
        indexOf(char, ignoreCase = ignoreCase) >= 0

/**
 * Returns `true` if this char sequence contains at least one match of the specified regular expression [regex].
 */
//@FixmeRegex
//@kotlin.internal.InlineOnly
//public inline operator fun CharSequence.contains(regex: Regex): Boolean = regex.containsMatchIn(this)


// rangesDelimitedBy


private class DelimitedRangesSequence(private val input: CharSequence, private val startIndex: Int, private val limit: Int, private val getNextMatch: CharSequence.(Int) -> Pair<Int, Int>?): Sequence<IntRange> {

    override fun iterator(): Iterator<IntRange> = object : Iterator<IntRange> {
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var currentStartIndex: Int = startIndex.coerceIn(0, input.length)
        var nextSearchIndex: Int = currentStartIndex
        var nextItem: IntRange? = null
        var counter: Int = 0

        private fun calcNext() {
            if (nextSearchIndex < 0) {
                nextState = 0
                nextItem = null
            }
            else {
                if (limit > 0 && ++counter >= limit || nextSearchIndex > input.length) {
                    nextItem = currentStartIndex..input.lastIndex
                    nextSearchIndex = -1
                }
                else {
                    val match = input.getNextMatch(nextSearchIndex)
                    if (match == null) {
                        nextItem = currentStartIndex..input.lastIndex
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
 * Returns a sequence of index ranges of substrings in this char sequence around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param startIndex The index to start searching delimiters from.
 *  No range having its start value less than [startIndex] is returned.
 *  [startIndex] is coerced to be non-negative and not greater than length of this string.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 */
private fun CharSequence.rangesDelimitedBy(delimiters: CharArray, startIndex: Int = 0, ignoreCase: Boolean = false, limit: Int = 0): Sequence<IntRange> {
    require(limit >= 0, { "Limit must be non-negative, but was $limit." })

    return DelimitedRangesSequence(this, startIndex, limit, { startIndex -> findAnyOf(delimiters, startIndex, ignoreCase = ignoreCase, last = false)?.let { it.first to 1 } })
}


/**
 * Returns a sequence of index ranges of substrings in this char sequence around occurrences of the specified [delimiters].
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
private fun CharSequence.rangesDelimitedBy(delimiters: Array<out String>, startIndex: Int = 0, ignoreCase: Boolean = false, limit: Int = 0): Sequence<IntRange> {
    require(limit >= 0, { "Limit must be non-negative, but was $limit." } )
    val delimitersList = delimiters.asList()

    return DelimitedRangesSequence(this, startIndex, limit, { startIndex -> findAnyOf(delimitersList, startIndex, ignoreCase = ignoreCase, last = false)?.let { it.first to it.second.length } })

}


// split

/**
 * Splits this char sequence to a sequence of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 */
public fun CharSequence.splitToSequence(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
        rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).map { substring(it) }

/**
 * Splits this char sequence to a list of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and matches at each position the first element in [delimiters]
 * that is equal to a delimiter in this instance at that position.
 */
public fun CharSequence.split(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String> =
        rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).asIterable().map { substring(it) }

/**
 * Splits this char sequence to a sequence of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 */
public fun CharSequence.splitToSequence(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
        rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).map { substring(it) }

/**
 * Splits this char sequence to a list of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 */
public fun CharSequence.split(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): List<String> =
        rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).asIterable().map { substring(it) }

/**
 * Splits this char sequence around matches of the given regular expression.
 *
 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 */
//@FixmeRegex
//@kotlin.internal.InlineOnly
//public inline fun CharSequence.split(regex: Regex, limit: Int = 0): List<String> = regex.split(this, limit)

/**
 * Splits this char sequence to a sequence of lines delimited by any of the following character sequences: CRLF, LF or CR.
 */
public fun CharSequence.lineSequence(): Sequence<String> = splitToSequence("\r\n", "\n", "\r")

/**
 * * Splits this char sequence to a list of lines delimited by any of the following character sequences: CRLF, LF or CR.
 */
public fun CharSequence.lines(): List<String> = lineSequence().toList()

// From _Strings.kt.
/**
 * Returns index of the first character matching the given [predicate], or -1 if the char sequence does not contain such character.
 */
public inline fun CharSequence.indexOfFirst(predicate: (Char) -> Boolean): Int {
    for (index in indices) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns index of the last character matching the given [predicate], or -1 if the char sequence does not contain such character.
 */
public inline fun CharSequence.indexOfLast(predicate: (Char) -> Boolean): Int {
    for (index in indices.reversed()) {
        if (predicate(this[index])) {
            return index
        }
    }
    return -1
}

/**
 * Returns the last character.
 * @throws [NoSuchElementException] if the char sequence is empty.
 */
public fun CharSequence.last(): Char {
    if (isEmpty())
        throw NoSuchElementException("Char sequence is empty.")
    return this[lastIndex]
}

/**
 * Returns the single character, or throws an exception if the char sequence is empty or has more than one character.
 */
public fun CharSequence.single(): Char {
    return when (length) {
        0 -> throw NoSuchElementException("Char sequence is empty.")
        1 -> this[0]
        else -> throw IllegalArgumentException("Char sequence has more than one element")
    }
}

/**
 * Returns the single character matching the given [predicate], or throws exception if there is no or more than one matching character.
 */
public inline fun CharSequence.single(predicate: (Char) -> Boolean): Char {
    var single: Char? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Char sequence contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Char sequence contains no character matching the predicate.")
    return single as Char
}

/**
 * Returns single character, or `null` if the char sequence is empty or has more than one character.
 */
public fun CharSequence.singleOrNull(): Char? {
    return if (length == 1) this[0] else null
}

/**
 * Returns the single character matching the given [predicate], or `null` if character was not found or more than one character was found.
 */
public inline fun CharSequence.singleOrNull(predicate: (Char) -> Boolean): Char? {
    var single: Char? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns first character.
 * @throws [NoSuchElementException] if the char sequence is empty.
 */
public fun CharSequence.first(): Char {
    if (isEmpty())
        throw NoSuchElementException("Char sequence is empty.")
    return this[0]
}

/**
 * Returns the first character matching the given [predicate].
 * @throws [NoSuchElementException] if no such character is found.
 */
public inline fun CharSequence.first(predicate: (Char) -> Boolean): Char {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Char sequence contains no character matching the predicate.")
}