/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

expect interface Appendable {
    fun append(c: Char): Appendable
    fun append(csq: CharSequence?): Appendable
    fun append(csq: CharSequence?, start: Int, end: Int): Appendable
}

expect class StringBuilder : Appendable, CharSequence {
    constructor()
    constructor(capacity: Int)
    constructor(content: CharSequence)

    override val length: Int
    override operator fun get(index: Int): Char
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence

    fun reverse(): StringBuilder
    override fun append(c: Char): StringBuilder
    override fun append(csq: CharSequence?): StringBuilder
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder
    fun append(obj: Any?): StringBuilder
}

expect class Regex {
    constructor(pattern: String)
    constructor(pattern: String, option: RegexOption)
    constructor(pattern: String, options: Set<RegexOption>)

    val pattern: String
    val options: Set<RegexOption>

    fun matchEntire(input: CharSequence): MatchResult?
    infix fun matches(input: CharSequence): Boolean
    fun containsMatchIn(input: CharSequence): Boolean
    fun replace(input: CharSequence, replacement: String): String
    fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String
    fun replaceFirst(input: CharSequence, replacement: String): String

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     */
    fun find(input: CharSequence, startIndex: Int = 0): MatchResult?

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     */
    fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult>

    /**
     * Splits the [input] CharSequence around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    fun split(input: CharSequence, limit: Int = 0): List<String>

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

expect fun Char.isWhitespace(): Boolean
expect fun Char.toLowerCase(): Char
expect fun Char.toUpperCase(): Char
expect fun Char.isHighSurrogate(): Boolean
expect fun Char.isLowSurrogate(): Boolean

// From string.kt


/**
 * Converts the characters in the specified array to a string.
 */
@SinceKotlin("1.2")
public expect fun String(chars: CharArray): String

/**
 * Converts the characters from a portion of the specified array to a string.
 */
@SinceKotlin("1.2")
public expect fun String(chars: CharArray, offset: Int, length: Int): String


internal expect fun String.nativeIndexOf(str: String, fromIndex: Int): Int
internal expect fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int


public expect fun String.substring(startIndex: Int): String
public expect fun String.substring(startIndex: Int, endIndex: Int): String

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 *
 * @sample samples.text.Strings.toUpperCase
 */
public expect fun String.toUpperCase(): String

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 *
 * @sample samples.text.Strings.toLowerCase
 */
public expect fun String.toLowerCase(): String
public expect fun String.capitalize(): String
public expect fun String.decapitalize(): String
public expect fun CharSequence.repeat(n: Int): String


/**
 * Clears the content of this string builder making it empty.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
public expect fun StringBuilder.clear(): StringBuilder


/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
expect fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
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
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
expect fun String?.equals(other: String?, ignoreCase: Boolean = false): Boolean

/**
 * Compares two strings lexicographically, optionally ignoring case differences.
 */
@SinceKotlin("1.2")
expect fun String.compareTo(other: String, ignoreCase: Boolean = false): Int


// From stringsCode.kt

internal inline expect fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int
internal inline expect fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

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
 * A Comparator that orders strings ignoring character case.
 *
 * Note that this Comparator does not take locale into account,
 * and will result in an unsatisfactory ordering for certain locales.
 */
@SinceKotlin("1.2")
public expect val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>


/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 */
expect fun String.toBoolean(): Boolean

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
expect fun String.toByte(): Byte

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
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
