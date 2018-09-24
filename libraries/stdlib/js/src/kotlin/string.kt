/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.js.RegExp

/**
 * Converts the characters in the specified array to a string.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun String(chars: CharArray): String {
    return js("String.fromCharCode").apply(null, chars)
}

/**
 * Converts the characters from a portion of the specified array to a string.
 */
@SinceKotlin("1.2")
public actual fun String(chars: CharArray, offset: Int, length: Int): String {
    return String(chars.copyOfRange(offset, offset + length))
}

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 *
 * @sample samples.text.Strings.toUpperCase
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toUpperCase(): String = asDynamic().toUpperCase()

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 *
 * @sample samples.text.Strings.toLowerCase
 */
@kotlin.internal.InlineOnly
public actual inline fun String.toLowerCase(): String = asDynamic().toLowerCase()

@kotlin.internal.InlineOnly
internal actual inline fun String.nativeIndexOf(str: String, fromIndex: Int): Int = asDynamic().indexOf(str, fromIndex)

@kotlin.internal.InlineOnly
internal actual inline fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int = asDynamic().lastIndexOf(str, fromIndex)

@kotlin.internal.InlineOnly
internal inline fun String.nativeStartsWith(s: String, position: Int): Boolean = asDynamic().startsWith(s, position)

@kotlin.internal.InlineOnly
internal inline fun String.nativeEndsWith(s: String): Boolean = asDynamic().endsWith(s)

@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int): String = asDynamic().substring(startIndex)

@kotlin.internal.InlineOnly
public actual inline fun String.substring(startIndex: Int, endIndex: Int): String = asDynamic().substring(startIndex, endIndex)

@kotlin.internal.InlineOnly
public inline fun String.concat(str: String): String = asDynamic().concat(str)

@kotlin.internal.InlineOnly
public inline fun String.match(regex: String): Array<String>? = asDynamic().match(regex)

//native public fun String.trim(): String
//TODO: String.replace to implement effective trimLeading and trimTrailing

@kotlin.internal.InlineOnly
internal inline fun String.nativeReplace(pattern: RegExp, replacement: String): String = asDynamic().replace(pattern, replacement)

@SinceKotlin("1.2")
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.compareTo(other: String, ignoreCase: Boolean = false): Int {
    if (ignoreCase) {
        val n1 = this.length
        val n2 = other.length
        val min = minOf(n1, n2)
        if (min == 0) return n1 - n2
        var start = 0
        while (true) {
            val end = minOf(start + 16, min)
            var s1 = this.substring(start, end)
            var s2 = other.substring(start, end)
            if (s1 != s2) {
                s1 = s1.toUpperCase()
                s2 = s2.toUpperCase()
                if (s1 != s2) {
                    s1 = s1.toLowerCase()
                    s2 = s2.toLowerCase()
                    if (s1 != s2) {
                        return s1.compareTo(s2)
                    }
                }
            }
            if (end == min) break
            start = end
        }
        return n1 - n2
    } else {
        return compareTo(other)
    }
}


private val STRING_CASE_INSENSITIVE_ORDER = Comparator<String> { a, b -> a.compareTo(b, ignoreCase = true) }

@SinceKotlin("1.2")
public actual val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>
    get() = STRING_CASE_INSENSITIVE_ORDER
