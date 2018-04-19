/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

internal expect fun String.nativeIndexOf(str: String, fromIndex: Int): Int
internal expect fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int


public expect fun String.substring(startIndex: Int): String
public expect fun String.substring(startIndex: Int, endIndex: Int): String


public expect fun String.toUpperCase(): String
public expect fun String.toLowerCase(): String
public expect fun String.capitalize(): String
public expect fun String.decapitalize(): String
public expect fun CharSequence.repeat(n: Int): String


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
expect fun CharSequence.regionMatches(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean = false): Boolean



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


@PublishedApi
internal expect fun checkRadix(radix: Int): Int
internal expect fun digitOf(char: Char, radix: Int): Int
