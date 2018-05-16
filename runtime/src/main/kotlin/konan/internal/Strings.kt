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

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
@SymbolName("Kotlin_String_indexOfChar")
external internal actual inline fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
@SymbolName("Kotlin_String_indexOfString")
external internal actual fun String.nativeIndexOf(str: String, fromIndex: Int): Int

/**
 * Returns the index within this string of the last occurrence of the specified character.
 */
@SymbolName("Kotlin_String_lastIndexOfChar")
external internal actual inline fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int

/**
 * Returns the index within this string of the last occurrence of the specified character, starting from the specified offset.
 */
@SymbolName("Kotlin_String_lastIndexOfString")
external internal actual fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int

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
external internal fun stringEqualsIgnoreCase(thiz: String, other: String): Boolean

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
@SymbolName("Kotlin_String_replace")
external public actual fun String.replace(
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
 * @sample samples.text.Strings.decaptialize
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
 * Returns the substring of this string starting at the [startIndex].
 *
 * @param startIndex the start index (inclusive).
 */
@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int): String =
        subSequence(startIndex, this.length) as String

/**
 * Returns `true` if this string starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean =
        regionMatches(0, prefix, 0, prefix.length, ignoreCase)

/**
 * Returns `true` if a substring of this string starting at the specified offset [startIndex] starts with the specified prefix.
 */
public fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean =
        regionMatches(startIndex, prefix, 0, prefix.length, ignoreCase)

/**
 * Returns `true` if this string ends with the specified suffix.
 */
public fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean =
        regionMatches(length - suffix.length, suffix, 0, suffix.length, ignoreCase)

/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
@SymbolName("Kotlin_CharSequence_regionMatches")
external public actual fun CharSequence.regionMatches(
        thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int,
        ignoreCase: Boolean): Boolean


/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param thisOffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other string of the substring to compare.
 * @param length the length of the substring to compare.
 */
@SymbolName("Kotlin_String_regionMatches")
external public fun String.regionMatches(
        thisOffset: Int, other: String, otherOffset: Int, length: Int,
        ignoreCase: Boolean = false): Boolean

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 */
@SymbolName("Kotlin_String_toUpperCase")
external public actual fun String.toUpperCase(): String

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 */
@SymbolName("Kotlin_String_toLowerCase")
@Suppress("NOTHING_TO_INLINE")
external public actual inline fun String.toLowerCase(): String

/**
 * Returns an array containing all characters of the specified string.
 */
@SymbolName("Kotlin_String_toCharArray")
external public fun String.toCharArray() : CharArray

/**
 * Returns a copy of this string having its first letter uppercased, or the original string,
 * if it's empty or already starts with an upper case letter.
 *
 * @sample samples.text.Strings.captialize
 */
public actual fun String.capitalize(): String {
    return if (isNotEmpty() && this[0].isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}