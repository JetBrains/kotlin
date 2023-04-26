/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

expect class Regex {
    constructor(pattern: String)
    constructor(pattern: String, option: RegexOption)
    constructor(pattern: String, options: Set<RegexOption>)

    val pattern: String
    val options: Set<RegexOption>

    fun matchEntire(input: CharSequence): MatchResult?
    infix fun matches(input: CharSequence): Boolean

    /**
     * Attempts to match a regular expression exactly at the specified [index] in the [input] char sequence.
     *
     * Unlike [matchEntire] function, it doesn't require the match to span to the end of [input].
     *
     * @return An instance of [MatchResult] if the input matches this [Regex] at the specified [index] or `null` otherwise.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of the [input] char sequence.
     * @sample samples.text.Regexps.matchAt
     */
    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun matchAt(input: CharSequence, index: Int): MatchResult?

    /**
     * Checks if a regular expression matches a part of the specified [input] char sequence
     * exactly at the specified [index].
     *
     * Unlike [matches] function, it doesn't require the match to span to the end of [input].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of the [input] char sequence.
     * @sample samples.text.Regexps.matchesAt
     */
    @SinceKotlin("1.7")
    @WasExperimental(ExperimentalStdlibApi::class)
    fun matchesAt(input: CharSequence, index: Int): Boolean

    fun containsMatchIn(input: CharSequence): Boolean
    fun replace(input: CharSequence, replacement: String): String
    fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String
    fun replaceFirst(input: CharSequence, replacement: String): String

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     * @sample samples.text.Regexps.find
     */
    fun find(input: CharSequence, startIndex: Int = 0): MatchResult?

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     *
     * @sample samples.text.Regexps.findAll
     */
    fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult>

    /**
     * Splits the [input] CharSequence to a list of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    fun split(input: CharSequence, limit: Int = 0): List<String>

    /**
     * Splits the [input] CharSequence to a sequence of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     * @sample samples.text.Regexps.splitToSequence
     */
    @SinceKotlin("1.6")
    @WasExperimental(ExperimentalStdlibApi::class)
    public fun splitToSequence(input: CharSequence, limit: Int = 0): Sequence<String>

    companion object {
        fun fromLiteral(literal: String): Regex
        fun escape(literal: String): String
        fun escapeReplacement(literal: String): String
    }
}

expect class MatchGroup {
    val value: String
}

expect enum class RegexOption {
    IGNORE_CASE,
    MULTILINE
}


// From char.kt

expect fun Char.isHighSurrogate(): Boolean
expect fun Char.isLowSurrogate(): Boolean

// From string.kt


/**
 * Converts the characters in the specified array to a string.
 */
@SinceKotlin("1.2")
@Deprecated("Use CharArray.concatToString() instead", ReplaceWith("chars.concatToString()"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5")
public expect fun String(chars: CharArray): String

/**
 * Converts the characters from a portion of the specified array to a string.
 *
 * @throws IndexOutOfBoundsException if either [offset] or [length] are less than zero
 * or `offset + length` is out of [chars] array bounds.
 */
@SinceKotlin("1.2")
@Deprecated("Use CharArray.concatToString(startIndex, endIndex) instead", ReplaceWith("chars.concatToString(offset, offset + length)"))
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.5")
public expect fun String(chars: CharArray, offset: Int, length: Int): String

/**
 * Concatenates characters in this [CharArray] into a String.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun CharArray.concatToString(): String

/**
 * Concatenates characters in this [CharArray] or its subrange into a String.
 *
 * @param startIndex the beginning (inclusive) of the subrange of characters, 0 by default.
 * @param endIndex the end (exclusive) of the subrange of characters, size of this array by default.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the size of this array.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun CharArray.concatToString(startIndex: Int = 0, endIndex: Int = this.size): String

/**
 * Returns a [CharArray] containing characters of this string.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun String.toCharArray(): CharArray

/**
 * Returns a [CharArray] containing characters of this string or its substring.
 *
 * @param startIndex the beginning (inclusive) of the substring, 0 by default.
 * @param endIndex the end (exclusive) of the substring, length of this string by default.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the length of this string.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun String.toCharArray(startIndex: Int = 0, endIndex: Int = this.length): CharArray

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun ByteArray.decodeToString(): String

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array or its subrange.
 *
 * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to decode, size of this array by default.
 * @param throwOnInvalidSequence specifies whether to throw an exception on malformed byte sequence or replace it by the replacement char `\uFFFD`.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the size of this array.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 * @throws CharacterCodingException if the byte array contains malformed UTF-8 byte sequence and [throwOnInvalidSequence] is true.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun ByteArray.decodeToString(
    startIndex: Int = 0,
    endIndex: Int = this.size,
    throwOnInvalidSequence: Boolean = false
): String

/**
 * Encodes this string to an array of bytes in UTF-8 encoding.
 *
 * Any malformed char sequence is replaced by the replacement byte sequence.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun String.encodeToByteArray(): ByteArray

/**
 * Encodes this string or its substring to an array of bytes in UTF-8 encoding.
 *
 * @param startIndex the beginning (inclusive) of the substring to encode, 0 by default.
 * @param endIndex the end (exclusive) of the substring to encode, length of this string by default.
 * @param throwOnInvalidSequence specifies whether to throw an exception on malformed char sequence or replace.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the length of this string.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 * @throws CharacterCodingException if this string contains malformed char sequence and [throwOnInvalidSequence] is true.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public expect fun String.encodeToByteArray(
    startIndex: Int = 0,
    endIndex: Int = this.length,
    throwOnInvalidSequence: Boolean = false
): ByteArray


internal expect fun String.nativeIndexOf(str: String, fromIndex: Int): Int
internal expect fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int


public expect fun String.substring(startIndex: Int): String
public expect fun String.substring(startIndex: Int, endIndex: Int): String

/**
 * Returns a string containing this char sequence repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample samples.text.Strings.repeat
 */
public expect fun CharSequence.repeat(n: Int): String


/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 * 
 * @sample samples.text.Strings.replace
 */
expect fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
 *
 * @sample samples.text.Strings.replace
 */
expect fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String

/**
 * Returns a new string with the first occurrence of [oldChar] replaced with [newChar].
 */
expect fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String

/**
 * Returns a new string obtained by replacing the first occurrence of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
expect fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String

/**
 * Returns `true` if this string is equal to [other], optionally ignoring character case.
 *
 * Two strings are considered to be equal if they have the same length and the same character at the same index.
 * If [ignoreCase] is true, the result of `Char.uppercaseChar().lowercaseChar()` on each character is compared.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
expect fun String?.equals(other: String?, ignoreCase: Boolean = false): Boolean

/**
 * Compares two strings lexicographically, optionally ignoring case differences.
 *
 * If [ignoreCase] is true, the result of `Char.uppercaseChar().lowercaseChar()` on each character is compared.
 */
@SinceKotlin("1.2")
expect fun String.compareTo(other: String, ignoreCase: Boolean = false): Int


public expect fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean
public expect fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean
public expect fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean

// From stringsCode.kt

internal expect fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int
internal expect fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

expect fun CharSequence.isBlank(): Boolean
/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
expect fun CharSequence.regionMatches(
    thisOffset: Int,
    other: CharSequence,
    otherOffset: Int,
    length: Int,
    ignoreCase: Boolean = false
): Boolean

/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param thisOffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other string of the substring to compare.
 * @param length the length of the substring to compare.
 */
@SinceKotlin("1.9")
public expect fun String.regionMatches(
    thisOffset: Int,
    other: String,
    otherOffset: Int,
    length: Int,
    ignoreCase: Boolean = false
): Boolean


/**
 * A Comparator that orders strings ignoring character case.
 *
 * Note that this Comparator does not take locale into account,
 * and will result in an unsatisfactory ordering for certain locales.
 */
@SinceKotlin("1.2")
public expect val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>

/**
 * Returns `true` if this string is not `null` and its content is equal to the word "true", ignoring case, and `false` otherwise.
 *
 * There are also strict versions of the function available on non-nullable String, [toBooleanStrict] and [toBooleanStrictOrNull].
 */
@SinceKotlin("1.4")
public expect fun String?.toBoolean(): Boolean

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
expect fun String.toByte(): Byte

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
expect fun String.toByte(radix: Int): Byte


/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
expect fun String.toShort(): Short

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
expect fun String.toShort(radix: Int): Short

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
expect fun String.toInt(): Int

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
expect fun String.toInt(radix: Int): Int

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
expect fun String.toLong(): Long

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
expect fun String.toLong(radix: Int): Long

/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
expect fun String.toDouble(): Double

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
expect fun String.toFloat(): Float

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
expect fun String.toDoubleOrNull(): Double?

/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
expect fun String.toFloatOrNull(): Float?

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
expect fun Byte.toString(radix: Int): String

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
expect fun Short.toString(radix: Int): String

/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
expect fun Int.toString(radix: Int): String

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
expect fun Long.toString(radix: Int): String

@PublishedApi
internal expect fun checkRadix(radix: Int): Int

internal expect fun digitOf(char: Char, radix: Int): Int
