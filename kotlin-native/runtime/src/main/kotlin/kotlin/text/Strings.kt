/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

import kotlin.native.concurrent.SharedImmutable

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
@SymbolName("Kotlin_String_indexOfChar")
internal actual external fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
@SymbolName("Kotlin_String_indexOfString")
internal actual external fun String.nativeIndexOf(str: String, fromIndex: Int): Int

/**
 * Returns the index within this string of the last occurrence of the specified character.
 */
@SymbolName("Kotlin_String_lastIndexOfChar")
internal actual external fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

/**
 * Returns the index within this string of the last occurrence of the specified character, starting from the specified offset.
 */
@SymbolName("Kotlin_String_lastIndexOfString")
internal actual external fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int

/**
 * Returns `true` if this string is equal to [other], optionally ignoring character case.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 */
public actual fun String?.equals(other: String?, ignoreCase: Boolean): Boolean {
    if (this === null)
        return other === null
    if (other === null)
        return false
    return if (!ignoreCase)
        this.equals(other)
    else if (length != other.length)
        false
    else
        unsafeRangeEqualsIgnoreCase(0, other, 0, length)
}

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
public actual fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean): String {
    return if (!ignoreCase)
        replace(oldChar, newChar)
    else
        replaceIgnoreCase(oldChar, newChar)
}

@SymbolName("Kotlin_String_replace")
private external fun String.replace(oldChar: Char, newChar: Char): String

@OptIn(ExperimentalStdlibApi::class)
private fun String.replaceIgnoreCase(oldChar: Char, newChar: Char): String {
    val charArray = CharArray(length)
    val oldCharLower = oldChar.lowercaseChar()

    for (index in 0 until length) {
        val thisChar = this[index]
        val thisCharLower = thisChar.lowercaseChar()
        charArray[index] = if (thisCharLower == oldCharLower) newChar else thisChar
    }

    return charArray.concatToString()
}

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
public actual fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean): String =
        splitToSequence(oldValue, ignoreCase = ignoreCase).joinToString(separator = newValue)

/**
 * Returns a new string with the first occurrence of [oldChar] replaced with [newChar].
 */
public actual fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean): String {
    val index = indexOf(oldChar, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + 1, newChar.toString())
}

/**
 * Returns a new string obtained by replacing the first occurrence of the [oldValue] substring in this string
 * with the specified [newValue] string.
 */
public actual fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean): String {
    val index = indexOf(oldValue, ignoreCase = ignoreCase)
    return if (index < 0) this else this.replaceRange(index, index + oldValue.length, newValue)
}

/**
 * Returns `true` if this string is empty or consists solely of whitespace characters.
 *
 * @sample samples.text.Strings.stringIsBlank
 */
public actual fun CharSequence.isBlank(): Boolean = length == 0 || indices.all { this[it].isWhitespace() }

/**
 * Returns the substring of this string starting at the [startIndex] and ending right before the [endIndex].
 *
 * @param startIndex the start index (inclusive).
 * @param endIndex the end index (exclusive).
 */
@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int, endIndex: Int): String =
        subSequence(startIndex, endIndex) as String

/**
 * Returns a substring of this string that starts at the specified [startIndex] and continues to the end of the string.
 */
@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int): String =
        subSequence(startIndex, this.length) as String

/**
 * Returns `true` if this string starts with the specified prefix.
 */
public actual fun String.startsWith(prefix: String, ignoreCase: Boolean): Boolean =
        regionMatches(0, prefix, 0, prefix.length, ignoreCase)

/**
 * Returns `true` if a substring of this string starting at the specified offset [startIndex] starts with the specified prefix.
 */
public actual fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean): Boolean =
        regionMatches(startIndex, prefix, 0, prefix.length, ignoreCase)

/**
 * Returns `true` if this string ends with the specified suffix.
 */
public actual fun String.endsWith(suffix: String, ignoreCase: Boolean): Boolean =
        regionMatches(length - suffix.length, suffix, 0, suffix.length, ignoreCase)

/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
public actual fun CharSequence.regionMatches(
        thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int,
        ignoreCase: Boolean): Boolean {
    return if (this is String && other is String) {
        this.regionMatches(thisOffset, other, otherOffset, length, ignoreCase)
    } else {
        regionMatchesImpl(thisOffset, other, otherOffset, length, ignoreCase)
    }
}

/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param thisOffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other string of the substring to compare.
 * @param length the length of the substring to compare.
 */
public fun String.regionMatches(
        thisOffset: Int, other: String, otherOffset: Int, length: Int,
        ignoreCase: Boolean = false): Boolean {
    if (length < 0 || thisOffset < 0 || otherOffset < 0
            || thisOffset + length > this.length
            || otherOffset + length > other.length) {
        return false
    }
    return if (!ignoreCase)
        unsafeRangeEquals(thisOffset, other, otherOffset, length)
    else
        unsafeRangeEqualsIgnoreCase(thisOffset, other, otherOffset, length)
}

// Bounds must be checked before calling this method
@SymbolName("Kotlin_String_unsafeRangeEquals")
private external fun String.unsafeRangeEquals(thisOffset: Int, other: String, otherOffset: Int, length: Int): Boolean

// Bounds must be checked before calling this method
@OptIn(ExperimentalStdlibApi::class)
private fun String.unsafeRangeEqualsIgnoreCase(thisOffset: Int, other: String, otherOffset: Int, length: Int): Boolean {
    for (index in 0 until length) {
        val thisCharLower = this[thisOffset + index].lowercaseChar()
        val otherCharLower = other[otherOffset + index].lowercaseChar()
        if (thisCharLower != otherCharLower) {
            return false
        }
    }
    return true
}

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 */
public actual fun String.toUpperCase(): String = uppercaseImpl()

/**
 * Returns a copy of this string converted to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.uppercase
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public actual fun String.uppercase(): String = uppercaseImpl()

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 */
public actual fun String.toLowerCase(): String = lowercaseImpl()

/**
 * Returns a copy of this string converted to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.lowercase
 */
@SinceKotlin("1.4")
@ExperimentalStdlibApi
public actual fun String.lowercase(): String = lowercaseImpl()

/**
 * Returns a [CharArray] containing characters of this string.
 */
public actual fun String.toCharArray(): CharArray = toCharArray(this, 0, length)

@SymbolName("Kotlin_String_toCharArray")
private external fun toCharArray(string: String, start: Int, size: Int): CharArray

/**
 * Returns a copy of this string having its first letter titlecased using the rules of the default locale,
 * or the original string if it's empty or already starts with a title case letter.
 *
 * The title case of a character is usually the same as its upper case with several exceptions.
 * The particular list of characters with the special title case form depends on the underlying platform.
 *
 * @sample samples.text.Strings.capitalize
 */
public actual fun String.capitalize(): String {
    return if (isNotEmpty() && this[0].isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string having its first letter lowercased using the rules of the default locale,
 * or the original string if it's empty or already starts with a lower case letter.
 *
 * @sample samples.text.Strings.decapitalize
 */
public actual fun String.decapitalize(): String {
    return if (isNotEmpty() && !this[0].isLowerCase()) substring(0, 1).toLowerCase() + substring(1) else this
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
                1 -> this[0].let { char -> CharArray(n) { char }.concatToString() }
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
 * Converts the characters in the specified array to a string.
 */
@Deprecated("Use CharArray.concatToString() instead", ReplaceWith("chars.concatToString()"))
public actual fun String(chars: CharArray): String = chars.concatToString()

/**
 * Converts the characters from a portion of the specified array to a string.
 *
 * @throws IndexOutOfBoundsException if either [offset] or [length] are less than zero
 * or `offset + length` is out of [chars] array bounds.
 */
@Deprecated("Use CharArray.concatToString(startIndex, endIndex) instead", ReplaceWith("chars.concatToString(offset, offset + length)"))
public actual fun String(chars: CharArray, offset: Int, length: Int): String {
    if (offset < 0 || length < 0 || offset + length > chars.size)
        throw ArrayIndexOutOfBoundsException()

    return unsafeStringFromCharArray(chars, offset, length)
}

/**
 * Concatenates characters in this [CharArray] into a String.
 */
@SinceKotlin("1.3")
public actual fun CharArray.concatToString(): String = unsafeStringFromCharArray(this, 0, size)

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
public actual fun CharArray.concatToString(startIndex: Int, endIndex: Int): String {
    checkBoundsIndexes(startIndex, endIndex, size)
    return unsafeStringFromCharArray(this, startIndex, endIndex - startIndex)
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
@SinceKotlin("1.3")
public actual fun String.toCharArray(startIndex: Int, endIndex: Int): CharArray {
    checkBoundsIndexes(startIndex, endIndex, length)
    return toCharArray(this, startIndex, endIndex - startIndex)
}

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
@SinceKotlin("1.3")
public actual fun ByteArray.decodeToString(): String = unsafeStringFromUtf8(0, size)

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
public actual fun ByteArray.decodeToString(startIndex: Int, endIndex: Int, throwOnInvalidSequence: Boolean): String {
    checkBoundsIndexes(startIndex, endIndex, size)
    return if (throwOnInvalidSequence)
        unsafeStringFromUtf8OrThrow(startIndex, endIndex - startIndex)
    else
        unsafeStringFromUtf8(startIndex, endIndex - startIndex)
}

/**
 * Encodes this string to an array of bytes in UTF-8 encoding.
 *
 * Any malformed char sequence is replaced by the replacement byte sequence.
 */
@SinceKotlin("1.3")
public actual fun String.encodeToByteArray(): ByteArray = unsafeStringToUtf8(0, length)

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
public actual fun String.encodeToByteArray(startIndex: Int, endIndex: Int, throwOnInvalidSequence: Boolean): ByteArray {
    checkBoundsIndexes(startIndex, endIndex, length)
    return if (throwOnInvalidSequence)
        unsafeStringToUtf8OrThrow(startIndex, endIndex - startIndex)
    else
        unsafeStringToUtf8(startIndex, endIndex - startIndex)
}

@OptIn(ExperimentalStdlibApi::class)
internal fun compareToIgnoreCase(thiz: String, other: String): Int {
    val length = minOf(thiz.length, other.length)

    for (index in 0 until length) {
        val thisLowerChar = thiz[index].lowercaseChar()
        val otherLowerChar = other[index].lowercaseChar()
        if (thisLowerChar != otherLowerChar) {
            return if (thisLowerChar < otherLowerChar) -1 else 1
        }
    }

    return if (thiz.length == other.length)
        0
    else if (thiz.length < other.length)
        -1
    else
        1
}

public actual fun String.compareTo(other: String, ignoreCase: Boolean): Int {
    return if (!ignoreCase) this.compareTo(other)
    else compareToIgnoreCase(this, other)
}

@SharedImmutable
private val STRING_CASE_INSENSITIVE_ORDER = Comparator<String> { a, b -> a.compareTo(b, ignoreCase = true) }

public actual val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>
    get() = STRING_CASE_INSENSITIVE_ORDER
