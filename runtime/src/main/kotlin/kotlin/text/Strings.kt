/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

import kotlin.comparisons.*

/**
 * Returns a string containing this char sequence repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample samples.text.Strings.repeat
 */
public actual fun CharSequence.repeat(n: Int): String {
    require (n >= 0) { "Count 'n' must be non-negative, but was $n." }

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
 */
public actual fun String(chars: CharArray, offset: Int, length: Int): String = fromCharArray(chars, offset, length)

@SymbolName("Kotlin_String_compareToIgnoreCase")
external fun compareToIgnoreCase(thiz:String, other:String):Int

actual fun String.compareTo(other: String, ignoreCase: Boolean): Int {
    return if (!ignoreCase) this.compareTo(other)
    else compareToIgnoreCase(this, other)
}

private val STRING_CASE_INSENSITIVE_ORDER = Comparator<String> { a, b -> a.compareTo(b, ignoreCase = true) }

actual val String.Companion.CASE_INSENSITIVE_ORDER: Comparator<String>
  get() = STRING_CASE_INSENSITIVE_ORDER