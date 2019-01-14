/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

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
    else
        stringEqualsIgnoreCase(this, other)
}

@SymbolName("Kotlin_String_equalsIgnoreCase")
internal external fun stringEqualsIgnoreCase(thiz: String, other: String): Boolean

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
@SymbolName("Kotlin_String_replace")
public actual external fun String.replace(
        oldChar: Char, newChar: Char, ignoreCase: Boolean): String


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
 * Returns a copy of this string having its first letter lowercased, or the original string,
 * if it's empty or already starts with a lower case letter.
 *
 * @sample samples.text.Strings.decapitalize
 */
public actual fun String.decapitalize(): String {
    return if (isNotEmpty() && this[0].isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Returns `true` if this string is empty or consists solely of whitespace characters.
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
@SymbolName("Kotlin_String_regionMatches")
public external fun String.regionMatches(
        thisOffset: Int, other: String, otherOffset: Int, length: Int,
        ignoreCase: Boolean = false): Boolean

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 */
@SymbolName("Kotlin_String_toUpperCase")
public actual external fun String.toUpperCase(): String

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 */
@SymbolName("Kotlin_String_toLowerCase")
@Suppress("NOTHING_TO_INLINE")
public actual external fun String.toLowerCase(): String

/**
 * Returns a new character array containing the characters from this string.
 */
@SymbolName("Kotlin_String_toCharArray")
public external fun String.toCharArray(): CharArray

/**
 * Returns a copy of this string having its first letter uppercased, or the original string,
 * if it's empty or already starts with an upper case letter.
 *
 * @sample samples.text.Strings.capitalize
 */
public actual fun String.capitalize(): String {
    return if (isNotEmpty() && this[0].isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
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
                1 -> this[0].let { char -> fromCharArray(CharArray(n) { char }, 0, n) }
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
public actual fun String(chars: CharArray): String = fromCharArray(chars, 0, chars.size)

/**
 * Converts the characters from a portion of the specified array to a string.
 *
 * @throws IndexOutOfBoundsException if either [offset] or [length] are less than zero
 * or `offset + length` is out of [chars] array bounds.
 */
public actual fun String(chars: CharArray, offset: Int, length: Int): String = fromCharArray(chars, offset, length)

@SymbolName("Kotlin_String_compareToIgnoreCase")
internal external fun compareToIgnoreCase(thiz: String, other: String): Int

public actual fun String.compareTo(other: String, ignoreCase: Boolean): Int {
    return if (!ignoreCase) this.compareTo(other)
    else compareToIgnoreCase(this, other)
}

private val STRING_CASE_INSENSITIVE_ORDER = Comparator<String> { a, b -> a.compareTo(b, ignoreCase = true) }

public actual val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>
    get() = STRING_CASE_INSENSITIVE_ORDER