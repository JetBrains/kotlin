/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

actual class Regex {
    actual constructor(pattern: String) { TODO("Wasm stdlib: Text") }
    actual constructor(pattern: String, option: RegexOption) { TODO("Wasm stdlib: Text") }
    actual constructor(pattern: String, options: Set<RegexOption>) { TODO("Wasm stdlib: Text") }

    actual val pattern: String = TODO("Wasm stdlib: Text")
    actual val options: Set<RegexOption> = TODO("Wasm stdlib: Text")

    actual fun matchEntire(input: CharSequence): MatchResult? = TODO("Wasm stdlib: Text")
    actual infix fun matches(input: CharSequence): Boolean = TODO("Wasm stdlib: Text")
    actual fun containsMatchIn(input: CharSequence): Boolean = TODO("Wasm stdlib: Text")
    actual fun replace(input: CharSequence, replacement: String): String = TODO("Wasm stdlib: Text")
    actual fun replace(input: CharSequence, transform: (MatchResult) -> CharSequence): String = TODO("Wasm stdlib: Text")
    actual fun replaceFirst(input: CharSequence, replacement: String): String = TODO("Wasm stdlib: Text")

    /**
     * Returns the first match of a regular expression in the [input], beginning at the specified [startIndex].
     *
     * @param startIndex An index to start search with, by default 0. Must be not less than zero and not greater than `input.length()`
     * @return An instance of [MatchResult] if match was found or `null` otherwise.
     * @sample samples.text.Regexps.find
     */
    actual fun find(input: CharSequence, startIndex: Int): MatchResult? = TODO("Wasm stdlib: Text")

    /**
     * Returns a sequence of all occurrences of a regular expression within the [input] string, beginning at the specified [startIndex].
     *
     * @sample samples.text.Regexps.findAll
     */
    actual fun findAll(input: CharSequence, startIndex: Int): Sequence<MatchResult> = TODO("Wasm stdlib: Text")

    /**
     * Splits the [input] CharSequence around matches of this regular expression.
     *
     * @param limit Non-negative value specifying the maximum number of substrings the string can be split to.
     * Zero by default means no limit is set.
     */
    actual fun split(input: CharSequence, limit: Int): List<String> = TODO("Wasm stdlib: Text")

    actual companion object {
        actual fun fromLiteral(literal: String): Regex = TODO("Wasm stdlib: Text")
        actual fun escape(literal: String): String = TODO("Wasm stdlib: Text")
        actual fun escapeReplacement(literal: String): String = TODO("Wasm stdlib: Text")
    }
}

actual class MatchGroup {
    actual val value: String = TODO("Wasm stdlib: Text")
}

actual enum class RegexOption {
    IGNORE_CASE,
    MULTILINE
}


// From char.kt

actual fun Char.isWhitespace(): Boolean = TODO("Wasm stdlib: Text")
actual fun Char.toLowerCase(): Char = TODO("Wasm stdlib: Text")
actual fun Char.toUpperCase(): Char = TODO("Wasm stdlib: Text")
actual fun Char.isHighSurrogate(): Boolean = TODO("Wasm stdlib: Text")
actual fun Char.isLowSurrogate(): Boolean = TODO("Wasm stdlib: Text")

// From string.kt


/**
 * Converts the characters in the specified array to a string.
 */
@SinceKotlin("1.2")
public actual fun String(chars: CharArray): String = TODO("Wasm stdlib: Text")

/**
 * Converts the characters from a portion of the specified array to a string.
 *
 * @throws IndexOutOfBoundsException if either [offset] or [length] are less than zero
 * or `offset + length` is out of [chars] array bounds.
 */
@SinceKotlin("1.2")
public actual fun String(chars: CharArray, offset: Int, length: Int): String = TODO("Wasm stdlib: Text")

/**
 * Concatenates characters in this [CharArray] into a String.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun CharArray.concatToString(): String = TODO("Wasm stdlib: Text")

/**
 * Concatenates characters in this [CharArray] or its subrange into a String.
 *
 * @param startIndex the beginning (inclusive) of the subrange of characters, 0 by default.
 * @param endIndex the end (exclusive) of the subrange of characters, size of this array by default.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the size of this array.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun CharArray.concatToString(startIndex: Int, endIndex: Int): String = TODO("Wasm stdlib: Text")

/**
 * Returns a [CharArray] containing characters of this string.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun String.toCharArray(): CharArray = TODO("Wasm stdlib: Text")

/**
 * Returns a [CharArray] containing characters of this string or its substring.
 *
 * @param startIndex the beginning (inclusive) of the substring, 0 by default.
 * @param endIndex the end (exclusive) of the substring, length of this string by default.
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or [endIndex] is greater than the length of this string.
 * @throws IllegalArgumentException if [startIndex] is greater than [endIndex].
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun String.toCharArray(startIndex: Int, endIndex: Int): CharArray = TODO("Wasm stdlib: Text")

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun ByteArray.decodeToString(): String = TODO("Wasm stdlib: Text")

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
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun ByteArray.decodeToString(
    startIndex: Int,
    endIndex: Int,
    throwOnInvalidSequence: Boolean
): String = TODO("Wasm stdlib: Text")

/**
 * Encodes this string to an array of bytes in UTF-8 encoding.
 *
 * Any malformed char sequence is replaced by the replacement byte sequence.
 */
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun String.encodeToByteArray(): ByteArray = TODO("Wasm stdlib: Text")

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
@SinceKotlin("1.3")
@ExperimentalStdlibApi
public actual fun String.encodeToByteArray(
    startIndex: Int,
    endIndex: Int,
    throwOnInvalidSequence: Boolean
): ByteArray = TODO("Wasm stdlib: Text")


internal actual fun String.nativeIndexOf(str: String, fromIndex: Int): Int = TODO("Wasm stdlib: Text")
internal actual fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = TODO("Wasm stdlib: Text")


public actual fun String.substring(startIndex: Int): String = TODO("Wasm stdlib: Text")
public actual fun String.substring(startIndex: Int, endIndex: Int): String = TODO("Wasm stdlib: Text")

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 *
 * @sample samples.text.Strings.toUpperCase
 */
public actual fun String.toUpperCase(): String = TODO("Wasm stdlib: Text")

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 *
 * @sample samples.text.Strings.toLowerCase
 */
public actual fun String.toLowerCase(): String = TODO("Wasm stdlib: Text")
public actual fun String.capitalize(): String = TODO("Wasm stdlib: Text")
public actual fun String.decapitalize(): String = TODO("Wasm stdlib: Text")
public actual fun CharSequence.repeat(n: Int): String = TODO("Wasm stdlib: Text")


/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
actual fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean): String = TODO("Wasm stdlib: Text")

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
actual fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean): String = TODO("Wasm stdlib: Text")

/**
 * Returns a new string with the first occurrence of [oldChar] replaced with [newChar].
 */
actual fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean): String = TODO("Wasm stdlib: Text")

/**
 * Returns a new string obtained by replacing the first occurrence of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
actual fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean): String = TODO("Wasm stdlib: Text")

/**
 * Returns `true` if this string is equal to [other], optionally ignoring character case.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
actual fun String?.equals(other: String?, ignoreCase: Boolean): Boolean = TODO("Wasm stdlib: Text")

/**
 * Compares two strings lexicographically, optionally ignoring case differences.
 */
@SinceKotlin("1.2")
actual fun String.compareTo(other: String, ignoreCase: Boolean): Int = TODO("Wasm stdlib: Text")


public actual fun String.startsWith(prefix: String, ignoreCase: Boolean): Boolean = TODO("Wasm stdlib: Text")
public actual fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean): Boolean = TODO("Wasm stdlib: Text")
public actual fun String.endsWith(suffix: String, ignoreCase: Boolean): Boolean = TODO("Wasm stdlib: Text")

// From stringsCode.kt

internal actual fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int = TODO("Wasm stdlib: Text")
internal actual fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int = TODO("Wasm stdlib: Text")

actual fun CharSequence.isBlank(): Boolean = TODO("Wasm stdlib: Text")
/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
actual fun CharSequence.regionMatches(
    thisOffset: Int,
    other: CharSequence,
    otherOffset: Int,
    length: Int,
    ignoreCase: Boolean
): Boolean = TODO("Wasm stdlib: Text")


/**
 * A Comparator that orders strings ignoring character case.
 *
 * Note that this Comparator does not take locale into account,
 * and will result in an unsatisfactory ordering for certain locales.
 */
@SinceKotlin("1.2")
public actual val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String> get() = TODO("Wasm stdlib: Text")

actual fun String.toBoolean(): Boolean = TODO("Wasm stdlib: Text")

/**
 * Returns `true` if the contents of this string is equal to the word "true", ignoring case, and `false` otherwise.
 */
actual fun String?.toBoolean(): Boolean = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
actual fun String.toByte(): Byte = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a signed [Byte] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
actual fun String.toByte(radix: Int): Byte = TODO("Wasm stdlib: Text")


/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
actual fun String.toShort(): Short = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a [Short] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
actual fun String.toShort(radix: Int): Short = TODO("Wasm stdlib: Text")

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
actual fun String.toInt(): Int = TODO("Wasm stdlib: Text")

/**
 * Parses the string as an [Int] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
actual fun String.toInt(radix: Int): Int = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
actual fun String.toLong(): Long = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a [Long] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 * @throws IllegalArgumentException when [radix] is not a valid radix for string to number conversion.
 */
actual fun String.toLong(radix: Int): Long = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a [Double] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
actual fun String.toDouble(): Double = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a [Float] number and returns the result.
 * @throws NumberFormatException if the string is not a valid representation of a number.
 */
actual fun String.toFloat(): Float = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a [Double] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
actual fun String.toDoubleOrNull(): Double? = TODO("Wasm stdlib: Text")

/**
 * Parses the string as a [Float] number and returns the result
 * or `null` if the string is not a valid representation of a number.
 */
actual fun String.toFloatOrNull(): Float? = TODO("Wasm stdlib: Text")

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
actual fun Byte.toString(radix: Int): String = TODO("Wasm stdlib: Text")

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
actual fun Short.toString(radix: Int): String = TODO("Wasm stdlib: Text")

/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
actual fun Int.toString(radix: Int): String = TODO("Wasm stdlib: Text")

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.2")
actual fun Long.toString(radix: Int): String = TODO("Wasm stdlib: Text")

@PublishedApi
internal actual fun checkRadix(radix: Int): Int = TODO("Wasm stdlib: Text")

internal actual fun digitOf(char: Char, radix: Int): Int = TODO("Wasm stdlib: Text")
