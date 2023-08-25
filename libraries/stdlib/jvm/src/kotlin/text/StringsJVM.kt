/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.text

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.regex.Pattern


/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
@kotlin.internal.InlineOnly
internal actual inline fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int = (this as java.lang.String).indexOf(ch.code, fromIndex)

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
@kotlin.internal.InlineOnly
internal actual inline fun String.nativeIndexOf(str: String, fromIndex: Int): Int = (this as java.lang.String).indexOf(str, fromIndex)

/**
 * Returns the index within this string of the last occurrence of the specified character.
 */
@kotlin.internal.InlineOnly
internal actual inline fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int = (this as java.lang.String).lastIndexOf(ch.code, fromIndex)

/**
 * Returns the index within this string of the last occurrence of the specified character, starting from the specified offset.
 */
@kotlin.internal.InlineOnly
internal actual inline fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = (this as java.lang.String).lastIndexOf(str, fromIndex)

/**
 * Returns `true` if this string is equal to [other], optionally ignoring character case.
 *
 * Two strings are considered to be equal if they have the same length and the same character at the same index.
 * If [ignoreCase] is true, the result of `Char.uppercaseChar().lowercaseChar()` on each character is compared.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String?.equals(other: String?, ignoreCase: Boolean = false): Boolean {
    if (this === null)
        return other === null
    return if (!ignoreCase)
        (this as java.lang.String).equals(other)
    else
        (this as java.lang.String).equalsIgnoreCase(other)
}

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 *
 * @sample samples.text.Strings.replace
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String {
    // prefer case-insensitive platform implementation
    if (!ignoreCase) return (this as java.lang.String).replace(oldChar, newChar)

    return buildString(length) {
        this@replace.forEach { c ->
            append(if (c.equals(oldChar, ignoreCase)) newChar else c)
        }
    }
}

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
 *
 * @sample samples.text.Strings.replace
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String {
    run {
        var occurrenceIndex: Int = indexOf(oldValue, 0, ignoreCase)
        // FAST PATH: no match
        if (occurrenceIndex < 0) return this

        val oldValueLength = oldValue.length
        val searchStep = oldValueLength.coerceAtLeast(1)
        val newLengthHint = length - oldValueLength + newValue.length
        if (newLengthHint < 0) throw OutOfMemoryError()
        val stringBuilder = StringBuilder(newLengthHint)

        var i = 0
        do {
            stringBuilder.append(this, i, occurrenceIndex).append(newValue)
            i = occurrenceIndex + oldValueLength
            if (occurrenceIndex >= length) break
            occurrenceIndex = indexOf(oldValue, occurrenceIndex + searchStep, ignoreCase)
        } while (occurrenceIndex > 0)
        return stringBuilder.append(this, i, length).toString()
    }
}

/**
 * Returns a new string with the first occurrence of [oldChar] replaced with [newChar].
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String {
    val index = indexOf(oldChar, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + 1, newChar.toString())
}

/**
 * Returns a new string obtained by replacing the first occurrence of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String {
    val index = indexOf(oldValue, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + oldValue.length, newValue)
}

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 */
@Deprecated("Use uppercase() instead.", ReplaceWith("uppercase(Locale.getDefault())", "java.util.Locale"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public actual inline fun String.toUpperCase(): String = (this as java.lang.String).toUpperCase()

/**
 * Returns a copy of this string converted to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.uppercase
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public actual inline fun String.uppercase(): String = (this as java.lang.String).toUpperCase(Locale.ROOT)

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 */
@Deprecated("Use lowercase() instead.", ReplaceWith("lowercase(Locale.getDefault())", "java.util.Locale"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public actual inline fun String.toLowerCase(): String = (this as java.lang.String).toLowerCase()

/**
 * Returns a copy of this string converted to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.lowercase
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public actual inline fun String.lowercase(): String = (this as java.lang.String).toLowerCase(Locale.ROOT)

/**
 * Concatenates characters in this [CharArray] into a String.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun CharArray.concatToString(): String {
    return String(this)
}

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
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun CharArray.concatToString(startIndex: Int = 0, endIndex: Int = this.size): String {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, this.size)
    return String(this, startIndex, endIndex - startIndex)
}

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
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.toCharArray(startIndex: Int = 0, endIndex: Int = this.length): CharArray {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)
    return toCharArray(CharArray(endIndex - startIndex), 0, startIndex, endIndex)
}

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun ByteArray.decodeToString(): String {
    return String(this)
}

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
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun ByteArray.decodeToString(
    startIndex: Int = 0,
    endIndex: Int = this.size,
    throwOnInvalidSequence: Boolean = false
): String {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, this.size)

    if (!throwOnInvalidSequence) {
        return String(this, startIndex, endIndex - startIndex)
    }

    val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    return decoder.decode(ByteBuffer.wrap(this, startIndex, endIndex - startIndex)).toString()
}

/**
 * Encodes this string to an array of bytes in UTF-8 encoding.
 *
 * Any malformed char sequence is replaced by the replacement byte sequence.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public actual fun String.encodeToByteArray(): ByteArray {
    return this.toByteArray(Charsets.UTF_8)
}

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
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.encodeToByteArray(
    startIndex: Int = 0,
    endIndex: Int = this.length,
    throwOnInvalidSequence: Boolean = false
): ByteArray {
    AbstractList.checkBoundsIndexes(startIndex, endIndex, length)

    if (!throwOnInvalidSequence) {
        return this.substring(startIndex, endIndex).toByteArray(Charsets.UTF_8)
    }

    val encoder = Charsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    val byteBuffer = encoder.encode(CharBuffer.wrap(this, startIndex, endIndex))
    return if (byteBuffer.hasArray() && byteBuffer.arrayOffset() == 0 && byteBuffer.remaining() == byteBuffer.array()!!.size) {
        byteBuffer.array()
    } else {
        ByteArray(byteBuffer.remaining()).also { byteBuffer.get(it) }
    }
}

/**
 * Returns a [CharArray] containing characters of this string.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toCharArray(): CharArray = (this as java.lang.String).toCharArray()

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
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
@kotlin.internal.InlineOnly
public actual inline fun String.toCharArray(
    destination: CharArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = length
): CharArray {
    (this as java.lang.String).getChars(startIndex, endIndex, destination, destinationOffset)
    return destination
}

/**
 * Uses this string as a format string and returns a string obtained by substituting the specified arguments,
 * using the default locale.
 */
@kotlin.internal.InlineOnly
public inline fun String.format(vararg args: Any?): String = java.lang.String.format(this, *args)

/**
 * Uses the provided [format] as a format string and returns a string obtained by substituting the specified arguments,
 * using the default locale.
 */
@kotlin.internal.InlineOnly
public inline fun String.Companion.format(format: String, vararg args: Any?): String = java.lang.String.format(format, *args)

/**
 * Uses this string as a format string and returns a string obtained by substituting the specified arguments,
 * using the specified locale. If [locale] is `null` then no localization is applied.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun String.format(locale: Locale?, vararg args: Any?): String = java.lang.String.format(locale, this, *args)

/**
 * Uses the provided [format] as a format string and returns a string obtained by substituting the specified arguments,
 * using the specified locale. If [locale] is `null` then no localization is applied.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
public inline fun String.Companion.format(locale: Locale?, format: String, vararg args: Any?): String =
    java.lang.String.format(locale, format, *args)

/**
 * Splits this char sequence around matches of the given regular expression.
 *
 * This function has two notable differences from the method [Pattern.split]:
 *   - the function returns the result as a `List<String>` rather than an `Array<String>`;
 *   - when the [limit] is not specified or specified as 0,
 *   this function doesn't drop trailing empty strings from the result.

 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 */
public fun CharSequence.split(regex: Pattern, limit: Int = 0): List<String> {
    requireNonNegativeLimit(limit)
    return regex.split(this, if (limit == 0) -1 else limit).asList()
}

/**
 * Returns a substring of this string that starts at the specified [startIndex] and continues to the end of the string.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int): String = (this as java.lang.String).substring(startIndex)

/**
 * Returns the substring of this string starting at the [startIndex] and ending right before the [endIndex].
 *
 * @param startIndex the start index (inclusive).
 * @param endIndex the end index (exclusive).
 */
@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int, endIndex: Int): String = (this as java.lang.String).substring(startIndex, endIndex)

/**
 * Returns `true` if this string starts with the specified prefix.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return (this as java.lang.String).startsWith(prefix)
    else
        return regionMatches(0, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if a substring of this string starting at the specified offset [startIndex] starts with the specified prefix.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return (this as java.lang.String).startsWith(prefix, startIndex)
    else
        return regionMatches(startIndex, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if this string ends with the specified suffix.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return (this as java.lang.String).endsWith(suffix)
    else
        return regionMatches(length - suffix.length, suffix, 0, suffix.length, ignoreCase = true)
}

// "constructors" for String

/**
 * Converts the data from a portion of the specified array of bytes to characters using the specified character set
 * and returns the conversion result as a string.
 *
 * @param bytes the source array for the conversion.
 * @param offset the offset in the array of the data to be converted.
 * @param length the number of bytes to be converted.
 * @param charset the character set to use.
 */
@kotlin.internal.InlineOnly
public inline fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String =
    java.lang.String(bytes, offset, length, charset) as String

/**
 * Converts the data from the specified array of bytes to characters using the specified character set
 * and returns the conversion result as a string.
 */
@kotlin.internal.InlineOnly
public inline fun String(bytes: ByteArray, charset: Charset): String = java.lang.String(bytes, charset) as String

/**
 * Converts the data from a portion of the specified array of bytes to characters using the UTF-8 character set
 * and returns the conversion result as a string.
 *
 * @param bytes the source array for the conversion.
 * @param offset the offset in the array of the data to be converted.
 * @param length the number of bytes to be converted.
 */
@kotlin.internal.InlineOnly
public inline fun String(bytes: ByteArray, offset: Int, length: Int): String =
    java.lang.String(bytes, offset, length, Charsets.UTF_8) as String

/**
 * Converts the data from the specified array of bytes to characters using the UTF-8 character set
 * and returns the conversion result as a string.
 */
@kotlin.internal.InlineOnly
public inline fun String(bytes: ByteArray): String =
    java.lang.String(bytes, Charsets.UTF_8) as String

/**
 * Converts the characters in the specified array to a string.
 */
@kotlin.internal.InlineOnly
public actual inline fun String(chars: CharArray): String =
    java.lang.String(chars) as String

/**
 * Converts the characters from a portion of the specified array to a string.
 *
 * @throws IndexOutOfBoundsException if either [offset] or [length] are less than zero
 * or `offset + length` is out of [chars] array bounds.
 */
@kotlin.internal.InlineOnly
public actual inline fun String(chars: CharArray, offset: Int, length: Int): String =
    java.lang.String(chars, offset, length) as String

/**
 * Converts the code points from a portion of the specified Unicode code point array to a string.
 */
@kotlin.internal.InlineOnly
public inline fun String(codePoints: IntArray, offset: Int, length: Int): String =
    java.lang.String(codePoints, offset, length) as String

/**
 * Converts the contents of the specified StringBuffer to a string.
 */
@kotlin.internal.InlineOnly
public inline fun String(stringBuffer: java.lang.StringBuffer): String =
    java.lang.String(stringBuffer) as String

/**
 * Converts the contents of the specified StringBuilder to a string.
 */
@kotlin.internal.InlineOnly
public inline fun String(stringBuilder: java.lang.StringBuilder): String =
    java.lang.String(stringBuilder) as String

/**
 * Returns the character (Unicode code point) at the specified index.
 */
@kotlin.internal.InlineOnly
public inline fun String.codePointAt(index: Int): Int = (this as java.lang.String).codePointAt(index)

/**
 * Returns the character (Unicode code point) before the specified index.
 */
@kotlin.internal.InlineOnly
public inline fun String.codePointBefore(index: Int): Int = (this as java.lang.String).codePointBefore(index)

/**
 * Returns the number of Unicode code points in the specified text range of this String.
 */
@kotlin.internal.InlineOnly
public inline fun String.codePointCount(beginIndex: Int, endIndex: Int): Int =
    (this as java.lang.String).codePointCount(beginIndex, endIndex)

/**
 * Compares two strings lexicographically, optionally ignoring case differences.
 *
 * If [ignoreCase] is true, the result of `Char.uppercaseChar().lowercaseChar()` on each character is compared.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.compareTo(other: String, ignoreCase: Boolean = false): Int {
    if (ignoreCase)
        return (this as java.lang.String).compareToIgnoreCase(other)
    else
        return (this as java.lang.String).compareTo(other)
}

/**
 * Returns `true` if this string is equal to the contents of the specified [CharSequence], `false` otherwise.
 *
 * Note that if the [CharSequence] argument is a [StringBuffer] then the comparison may be performed in a synchronized block
 * that acquires that [StringBuffer]'s monitor.
 */
@kotlin.internal.InlineOnly
public inline fun String.contentEquals(charSequence: CharSequence): Boolean = (this as java.lang.String).contentEquals(charSequence)

/**
 * Returns `true` if this string is equal to the contents of the specified [StringBuffer], `false` otherwise.
 *
 * This function compares this string and the specified [StringBuffer] in a synchronized block
 * that acquires that [StringBuffer]'s monitor.
 */
@kotlin.internal.InlineOnly
public inline fun String.contentEquals(stringBuilder: StringBuffer): Boolean = (this as java.lang.String).contentEquals(stringBuilder)

/**
 * Returns `true` if the contents of this char sequence are equal to the contents of the specified [other],
 * i.e. both char sequences contain the same number of the same characters in the same order.
 *
 * If this [CharSequence] is a [String] and [other] is not `null`
 * then this function behaves the same as [String.contentEquals].
 *
 * @sample samples.text.Strings.contentEquals
 */
@SinceKotlin("1.5")
public actual infix fun CharSequence?.contentEquals(other: CharSequence?): Boolean {
    return if (this is String && other != null)
        contentEquals(other)
    else
        contentEqualsImpl(other)
}

/**
 * Returns `true` if the contents of this char sequence are equal to the contents of the specified [other], optionally ignoring case difference.
 *
 * If this [CharSequence] is a [String], [other] is not `null` and [ignoreCase] is `false`
 * then this function behaves the same as [String.contentEquals].
 *
 * @param ignoreCase `true` to ignore character case when comparing contents.
 *
 * @sample samples.text.Strings.contentEquals
 */
@SinceKotlin("1.5")
public actual fun CharSequence?.contentEquals(other: CharSequence?, ignoreCase: Boolean): Boolean {
    return if (ignoreCase)
        contentEqualsIgnoreCaseImpl(other)
    else
        contentEquals(other)
}

/**
 * Returns a canonical representation for this string object.
 */
@kotlin.internal.InlineOnly
public inline fun String.intern(): String = (this as java.lang.String).intern()

/**
 * Returns `true` if this string is empty or consists solely of whitespace characters.
 *
 * @sample samples.text.Strings.stringIsBlank
 */
public actual fun CharSequence.isBlank(): Boolean = length == 0 || indices.all { this[it].isWhitespace() }

/**
 * Returns the index within this string that is offset from the given [index] by [codePointOffset] code points.
 */
@kotlin.internal.InlineOnly
public inline fun String.offsetByCodePoints(index: Int, codePointOffset: Int): Int =
    (this as java.lang.String).offsetByCodePoints(index, codePointOffset)

/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun CharSequence.regionMatches(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean = false): Boolean {
    if (this is String && other is String)
        return this.regionMatches(thisOffset, other, otherOffset, length, ignoreCase)
    else
        return regionMatchesImpl(thisOffset, other, otherOffset, length, ignoreCase)
}

/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param thisOffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other string of the substring to compare.
 * @param length the length of the substring to compare.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.regionMatches(thisOffset: Int, other: String, otherOffset: Int, length: Int, ignoreCase: Boolean = false): Boolean =
    if (!ignoreCase)
        (this as java.lang.String).regionMatches(thisOffset, other, otherOffset, length)
    else
        (this as java.lang.String).regionMatches(ignoreCase, thisOffset, other, otherOffset, length)

/**
 * Returns a copy of this string converted to lower case using the rules of the specified locale.
 */
@Deprecated("Use lowercase() instead.", ReplaceWith("lowercase(locale)"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public inline fun String.toLowerCase(locale: java.util.Locale): String = lowercase(locale)

/**
 * Returns a copy of this string converted to lower case using the rules of the specified [locale].
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.lowercaseLocale
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public inline fun String.lowercase(locale: Locale): String = (this as java.lang.String).toLowerCase(locale)

/**
 * Returns a copy of this string converted to upper case using the rules of the specified locale.
 */
@Deprecated("Use uppercase() instead.", ReplaceWith("uppercase(locale)"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@kotlin.internal.InlineOnly
public inline fun String.toUpperCase(locale: java.util.Locale): String = uppercase(locale)

/**
 * Returns a copy of this string converted to upper case using the rules of the specified [locale].
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.uppercaseLocale
 */
@SinceKotlin("1.5")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.InlineOnly
public inline fun String.uppercase(locale: Locale): String = (this as java.lang.String).toUpperCase(locale)

/**
 * Encodes the contents of this string using the specified character set and returns the resulting byte array.
 * @sample samples.text.Strings.stringToByteArray
 */
@kotlin.internal.InlineOnly
public inline fun String.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = (this as java.lang.String).getBytes(charset)

/**
 * Converts the string into a regular expression [Pattern] optionally
 * with the specified [flags] from [Pattern] or'd together
 * so that strings can be split or matched on.
 */
@kotlin.internal.InlineOnly
public inline fun String.toPattern(flags: Int = 0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags)
}

/**
 * Returns a copy of this string having its first letter titlecased using the rules of the default locale,
 * or the original string if it's empty or already starts with a title case letter.
 *
 * The title case of a character is usually the same as its upper case with several exceptions.
 * The particular list of characters with the special title case form depends on the underlying platform.
 *
 * @sample samples.text.Strings.capitalize
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }", "java.util.Locale"))
@DeprecatedSinceKotlin(warningSince = "1.5")
public actual fun String.capitalize(): String {
    @Suppress("DEPRECATION")
    return capitalize(Locale.getDefault())
}

/**
 * Returns a copy of this string having its first letter titlecased using the rules of the specified [locale],
 * or the original string if it's empty or already starts with a title case letter.
 *
 * The title case of a character is usually the same as its upper case with several exceptions.
 * The particular list of characters with the special title case form depends on the underlying platform.
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.LowPriorityInOverloadResolution // To avoid conflicts in function references, as this function was introduced later than common capitalize()
public fun String.capitalize(locale: Locale): String {
    if (isNotEmpty()) {
        val firstChar = this[0]
        if (firstChar.isLowerCase()) {
            return buildString {
                val titleChar = firstChar.titlecaseChar()
                if (titleChar != firstChar.uppercaseChar()) {
                    append(titleChar)
                } else {
                    append(this@capitalize.substring(0, 1).uppercase(locale))
                }
                append(this@capitalize.substring(1))
            }
        }
    }
    return this
}

/**
 * Returns a copy of this string having its first letter lowercased using the rules of the default locale,
 * or the original string if it's empty or already starts with a lower case letter.
 *
 * @sample samples.text.Strings.decapitalize
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { it.lowercase(Locale.getDefault()) }", "java.util.Locale"))
@DeprecatedSinceKotlin(warningSince = "1.5")
public actual fun String.decapitalize(): String {
    @Suppress("DEPRECATION")
    return if (isNotEmpty() && !this[0].isLowerCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Returns a copy of this string having its first letter lowercased using the rules of the specified [locale],
 * or the original string, if it's empty or already starts with a lower case letter.
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { it.lowercase(locale) }"))
@DeprecatedSinceKotlin(warningSince = "1.5")
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
@kotlin.internal.LowPriorityInOverloadResolution // To avoid conflicts in function references, as this function was introduced later than common decapitalize()
public fun String.decapitalize(locale: Locale): String {
    return if (isNotEmpty() && !this[0].isLowerCase()) substring(0, 1).lowercase(locale) + substring(1) else this
}

/**
 * Returns a string containing this char sequence repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample samples.text.Strings.repeat
 */
public actual fun CharSequence.repeat(n: Int): String {
    require(n >= 0) { "Count 'n' must be non-negative, but was $n." }

    return when (n) {
        0 -> ""
        1 -> this.toString()
        else -> {
            when (length) {
                0 -> ""
                1 -> this[0].let { char -> String(CharArray(n) { char }) }
                else -> {
                    val sb = StringBuilder(n * length)
                    for (i in 1..n) {
                        sb.append(this)
                    }
                    sb.toString()
                }
            }
        }
    }
}


/**
 * A Comparator that orders strings ignoring character case.
 *
 * Note that this Comparator does not take locale into account,
 * and will result in an unsatisfactory ordering for certain locales.
 */
public actual val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>
    get() = java.lang.String.CASE_INSENSITIVE_ORDER
