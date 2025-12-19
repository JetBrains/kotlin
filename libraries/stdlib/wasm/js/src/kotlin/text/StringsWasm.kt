/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
internal actual fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int =
    nativeIndexOf(ch.toString(), fromIndex)

@OptIn(ExperimentalWasmJsInterop::class)
private fun nativeIndexOf(source: String, substring: String, fromIndex: Int): Int =
    js("source.indexOf(substring, fromIndex)")

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
internal actual fun String.nativeIndexOf(str: String, fromIndex: Int): Int =
    nativeIndexOf(this, str, fromIndex)

@OptIn(ExperimentalWasmJsInterop::class)
private fun nativeRepeat(str: String, n: Int): String =
    js("str.repeat(n)")

/**
 * Returns a string containing this char sequence repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample samples.text.Strings.repeat
 */
public actual fun CharSequence.repeat(n: Int): String {
    require(n >= 0) { "Count 'n' must be non-negative, but was $n." }

    return nativeRepeat(this.toString(), n)
}
