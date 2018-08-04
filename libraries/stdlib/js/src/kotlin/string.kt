/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

import kotlin.js.RegExp
import kotlin.math.min

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


@kotlin.internal.InlineOnly
public actual inline fun String.toUpperCase(): String = asDynamic().toUpperCase()

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

@Deprecated("Use length property instead.", ReplaceWith("length"), level = DeprecationLevel.ERROR)
@kotlin.internal.InlineOnly
public inline val CharSequence.size: Int get() = length

@kotlin.internal.InlineOnly
internal inline fun String.nativeReplace(pattern: RegExp, replacement: String): String = asDynamic().replace(pattern, replacement)

@kotlin.internal.InlineOnly
public actual inline fun String.compareTo(other: String, ignoreCase: Boolean): Int {
    if (ignoreCase) {
        val n1 = this.length
        val n2 = other.length
        val min = min(n1, n2) - 1
        for (i in 0..min) {
            var c1 = this[i]
            var c2 = other[i]
            if (c1 != c2) {
                c1 = c1.toUpperCase()
                c2 = c2.toUpperCase()
                if (c1 != c2) {
                    c1 = c1.toLowerCase()
                    c2 = c2.toLowerCase()
                    if (c1 != c2) {
                        return c1 - c2
                    }
                }
            }
        }
        return n1 - n2
    } else {
        return compareTo(other)
    }
}