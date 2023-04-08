/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.js.RegExp

@kotlin.internal.InlineOnly
internal actual inline fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int = nativeIndexOf(ch.toString(), fromIndex)

@kotlin.internal.InlineOnly
internal actual inline fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int = nativeLastIndexOf(ch.toString(), fromIndex)

/**
 * Returns `true` if this string starts with the specified prefix.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.startsWith(prefix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeStartsWith(prefix, 0)
    else
        return regionMatches(0, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if a substring of this string starting at the specified offset [startIndex] starts with the specified prefix.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.startsWith(prefix: String, startIndex: Int, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeStartsWith(prefix, startIndex)
    else
        return regionMatches(startIndex, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if this string ends with the specified suffix.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.endsWith(suffix: String, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase)
        return nativeEndsWith(suffix)
    else
        return regionMatches(length - suffix.length, suffix, 0, suffix.length, ignoreCase)
}

@Deprecated("Use Regex.matches() instead", ReplaceWith("regex.toRegex().matches(this)"))
@DeprecatedSinceKotlin(warningSince = "1.6")
public fun String.matches(regex: String): Boolean {
    @Suppress("DEPRECATION")
    val result = this.match(regex)
    return result != null && result.size != 0
}

/**
 * Returns `true` if this string is empty or consists solely of whitespace characters.
 *
 * @sample samples.text.Strings.stringIsBlank
 */
public actual fun CharSequence.isBlank(): Boolean = length == 0 || indices.all { this[it].isWhitespace() }

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
    if (this == null) return other == null
    if (other == null) return false
    if (!ignoreCase) return this == other

    if (this.length != other.length) return false

    for (index in 0 until this.length) {
        val thisChar = this[index]
        val otherChar = other[index]
        if (!thisChar.equals(otherChar, ignoreCase)) {
            return false
        }
    }

    return true
}


/**
 * Returns `true` if the specified range in this char sequence is equal to the specified range in another char sequence.
 * @param thisOffset the start offset in this char sequence of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other char sequence of the substring to compare.
 * @param length the length of the substring to compare.
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun CharSequence.regionMatches(
    thisOffset: Int,
    other: CharSequence,
    otherOffset: Int,
    length: Int,
    ignoreCase: Boolean = false
): Boolean = regionMatchesImpl(thisOffset, other, otherOffset, length, ignoreCase)

/**
 * Returns `true` if the specified range in this string is equal to the specified range in another string.
 * @param thisOffset the start offset in this string of the substring to compare.
 * @param other the string against a substring of which the comparison is performed.
 * @param otherOffset the start offset in the other string of the substring to compare.
 * @param length the length of the substring to compare.
 */
@SinceKotlin("1.9")
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.regionMatches(
    thisOffset: Int,
    other: String,
    otherOffset: Int,
    length: Int,
    ignoreCase: Boolean = false
): Boolean = regionMatchesImpl(thisOffset, other, otherOffset, length, ignoreCase)


/**
 * Returns a copy of this string having its first letter titlecased using the rules of the default locale,
 * or the original string if it's empty or already starts with a title case letter.
 *
 * The title case of a character is usually the same as its upper case with several exceptions.
 * The particular list of characters with the special title case form depends on the underlying platform.
 *
 * @sample samples.text.Strings.capitalize
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }"))
@DeprecatedSinceKotlin(warningSince = "1.5")
public actual fun String.capitalize(): String {
    return if (isNotEmpty()) substring(0, 1).uppercase() + substring(1) else this
}

/**
 * Returns a copy of this string having its first letter lowercased using the rules of the default locale,
 * or the original string if it's empty or already starts with a lower case letter.
 *
 * @sample samples.text.Strings.decapitalize
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { it.lowercase() }"))
@DeprecatedSinceKotlin(warningSince = "1.5")
public actual fun String.decapitalize(): String {
    return if (isNotEmpty()) substring(0, 1).lowercase() + substring(1) else this
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
            var result = ""
            if (!isEmpty()) {
                var s = this.toString()
                var count = n
                while (true) {
                    if ((count and 1) == 1) {
                        result += s
                    }
                    count = count ushr 1
                    if (count == 0) {
                        break
                    }
                    s += s
                }
            }
            return result
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
public actual fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
    nativeReplace(RegExp(Regex.escape(oldValue), if (ignoreCase) "gui" else "gu"), Regex.nativeEscapeReplacement(newValue))

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 *
 * @sample samples.text.Strings.replace
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
    nativeReplace(RegExp(Regex.escape(oldChar.toString()), if (ignoreCase) "gui" else "gu"), newChar.toString())

@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replaceFirst(oldValue: String, newValue: String, ignoreCase: Boolean = false): String =
    nativeReplace(RegExp(Regex.escape(oldValue), if (ignoreCase) "ui" else "u"), Regex.nativeEscapeReplacement(newValue))

@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replaceFirst(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String =
    nativeReplace(RegExp(Regex.escape(oldChar.toString()), if (ignoreCase) "ui" else "u"), newChar.toString())
