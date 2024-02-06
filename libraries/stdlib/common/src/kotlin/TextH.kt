/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Represents a compiled regular expression.
 * Provides functions to match strings in text with a pattern, replace the found occurrences and split text around matches.
 *
 * Note that the [pattern] syntax and the option set has differences on each platform.
 * See the docs of `Regex` for the specific platform for details.
 */
public expect class Regex {
    /** Creates a regular expression from the specified [pattern] string and the default options.  */
    public constructor(pattern: String)

    /** Creates a regular expression from the specified [pattern] string and the specified single [option].  */
    public constructor(pattern: String, option: RegexOption)

    /** Creates a regular expression from the specified [pattern] string and the specified set of [options].  */
    public constructor(pattern: String, options: Set<RegexOption>)

    /** The pattern string of this regular expression. */
    public val pattern: String

    /** The set of options that were used to create this regular expression. */
    public val options: Set<RegexOption>

    /**
     * Attempts to match the entire [input] CharSequence against the pattern.
     *
     * @return An instance of [MatchResult] if the entire input matches or `null` otherwise.
     */
    public fun matchEntire(input: CharSequence): MatchResult?

    /** Indicates whether the regular expression matches the entire [input]. */
    public infix fun matches(input: CharSequence): Boolean

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
    public fun matchAt(input: CharSequence, index: Int): MatchResult?

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
    public fun matchesAt(input: CharSequence, index: Int): Boolean

    /** Indicates whether the regular expression can find at least one match in the specified [input]. */
    public fun containsMatchIn(input: CharSequence): Boolean

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * The replacement string may contain references to the captured groups during a match. Occurrences of `${name}` or `$index`
     * in the replacement string will be substituted with the subsequences corresponding to the captured groups with the specified name or index.
     * In case of `$index`, the first digit after '$' is always treated as a part of group reference. Subsequent digits are incorporated
     * into `index` only if they would form a valid group reference. Only the digits '0'..'9' are considered as potential components
     * of the group reference. Note that indexes of captured groups start from 1, and the group with index 0 is the whole match.
     * In case of `${name}`, the `name` can consist of latin letters 'a'..'z' and 'A'..'Z', or digits '0'..'9'. The first character must be
     * a letter.
     *
     * Backslash character '\' can be used to include the succeeding character as a literal in the replacement string, e.g, `\$` or `\\`.
     * [Regex.escapeReplacement] can be used if [replacement] have to be treated as a literal string.
     *
     * @param input the char sequence to find matches of this regular expression in
     * @param replacement the expression to replace found matches with
     * @return the result of replacing each occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    public fun replace(input: CharSequence, replacement: String): String

    /**
     * Replaces all occurrences of this regular expression in the specified [input] string with the result of
     * the given function [transform] that takes [MatchResult] and returns a string to be used as a
     * replacement for that match.
     */
    public fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String

    /**
     * Replaces the first occurrence of this regular expression in the specified [input] string with specified [replacement] expression.
     *
     * The replacement string may contain references to the captured groups during a match. Occurrences of `${name}` or `$index`
     * in the replacement string will be substituted with the subsequences corresponding to the captured groups with the specified name or index.
     * In case of `$index`, the first digit after '$' is always treated as a part of group reference. Subsequent digits are incorporated
     * into `index` only if they would form a valid group reference. Only the digits '0'..'9' are considered as potential components
     * of the group reference. Note that indexes of captured groups start from 1, and the group with index 0 is the whole match.
     * In case of `${name}`, the `name` can consist of latin letters 'a'..'z' and 'A'..'Z', or digits '0'..'9'. The first character must be
     * a letter.
     *
     * Backslash character '\' can be used to include the succeeding character as a literal in the replacement string, e.g, `\$` or `\\`.
     * [Regex.escapeReplacement] can be used if [replacement] have to be treated as a literal string.
     *
     * @param input the char sequence to find a match of this regular expression in
     * @param replacement the expression to replace the found match with
     * @return the result of replacing the first occurrence of this regular expression in [input] with the result of evaluating the [replacement] expression
     * @throws RuntimeException if [replacement] expression is malformed, or capturing group with specified `name` or `index` does not exist
     */
    public fun replaceFirst(input: CharSequence, replacement: String): String

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     * @sample samples.text.Regexps.find
     */
    public fun find(input: CharSequence, startIndex: Int = 0): MatchResult?

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of the [input] char sequence.
     *
     * @sample samples.text.Regexps.findAll
     */
    public fun findAll(input: CharSequence, startIndex: Int = 0): Sequence<MatchResult>

    /**
     * Splits the [input] CharSequence to a list of strings around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    public fun split(input: CharSequence, limit: Int = 0): List<String>

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

    public companion object {
        /**
         * Returns a regular expression that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        public fun fromLiteral(literal: String): Regex

        /**
         * Returns a regular expression pattern string that matches the specified [literal] string literally.
         * No characters of that string will have special meaning when searching for an occurrence of the regular expression.
         */
        public fun escape(literal: String): String

        /**
         * Returns a literal replacement expression for the specified [literal] string.
         * No characters of that string will have special meaning when it is used as a replacement string in [Regex.replace] function.
         */
        public fun escapeReplacement(literal: String): String
    }
}

/**
 * Represents the results from a single capturing group within a [MatchResult] of [Regex].
 *
 * Note `MatchGroup` on particular platforms can also provide the `range` in the input
 * where the group match has happened.
 * See the docs of `MatchGroup` for the specific platform for details.
 *
 * @property value The value of captured group.
 */
public expect class MatchGroup {
    public val value: String
}

/**
 * Provides enumeration values to use to set regular expression options.
 *
 * Note that the enum has different set of possible options on different platforms and
 * can be extended later. Avoid exclusive `when` matches on this enum values.
 * See the docs of `RegexOption` for the specific platform for details.
 *
 */
public expect enum class RegexOption {
    /** Enables case-insensitive matching. Case comparison is Unicode-aware. */
    IGNORE_CASE,

    /** Enables multiline mode.
     *
     * In multiline mode the expressions `^` and `$` match just after or just before,
     * respectively, a line terminator or the end of the input sequence. */
    MULTILINE
}


// From char.kt

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
public expect fun Char.isHighSurrogate(): Boolean

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
public expect fun Char.isLowSurrogate(): Boolean


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
public expect fun CharArray.concatToString(startIndex: Int = 0, endIndex: Int = this.size): String

/**
 * Returns a [CharArray] containing characters of this string.
 */
@SinceKotlin("1.4")
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
public expect fun String.toCharArray(startIndex: Int = 0, endIndex: Int = this.length): CharArray

/**
 * Copies characters from this string into the [destination] character array and returns that array.
 *
 * @param destination the array to copy to.
 * @param destinationOffset the position in the array to copy to.
 * @param startIndex the start offset (inclusive) of the substring to copy.
 * @param endIndex the end offset (exclusive) of the substring to copy.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
 *  or when that index is out of the [destination] array indices range.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.9")
public expect fun String.toCharArray(
    destination: CharArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = length
): CharArray

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
@SinceKotlin("1.4")
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
public expect fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
 *
 * @sample samples.text.Strings.replace
 */
public expect fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String

/**
 * Returns a new string with the first occurrence of [oldChar] replaced with [newChar].
 */
public expect fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String

/**
 * Returns a new string obtained by replacing the first occurrence of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
public expect fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String

/**
 * Returns `true` if this string is equal to [other], optionally ignoring character case.
 *
 * Two strings are considered to be equal if they have the same length and the same character at the same index.
 * If [ignoreCase] is true, the result of `Char.uppercaseChar().lowercaseChar()` on each character is compared.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
public expect fun String?.equals(other: String?, ignoreCase: Boolean = false): Boolean

/**
 * Compares two strings lexicographically, optionally ignoring case differences.
 *
 * If [ignoreCase] is true, the result of `Char.uppercaseChar().lowercaseChar()` on each character is compared.
 */
@SinceKotlin("1.2")
public expect fun String.compareTo(other: String, ignoreCase: Boolean = false): Int


public expect fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean
public expect fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean
public expect fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean

// From stringsCode.kt

internal expect fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int
internal expect fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
public expect fun CharSequence.regionMatches(
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
public expect fun String.toByte(): Byte

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public expect fun String.toByte(radix: Int): Byte


/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public expect fun String.toShort(): Short

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public expect fun String.toShort(radix: Int): Short

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public expect fun String.toInt(): Int

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public expect fun String.toInt(radix: Int): Int

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public expect fun String.toLong(): Long

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
public expect fun String.toLong(radix: Int): Long

/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public expect fun String.toDouble(): Double

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
public expect fun String.toFloat(): Float

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public expect fun String.toDoubleOrNull(): Double?

/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
public expect fun String.toFloatOrNull(): Float?

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public expect fun Byte.toString(radix: Int): String

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public expect fun Short.toString(radix: Int): String

/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public expect fun Int.toString(radix: Int): String

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
public expect fun Long.toString(radix: Int): String

@PublishedApi
internal expect fun checkRadix(radix: Int): Int

internal expect fun digitOf(char: Char, radix: Int): Int
