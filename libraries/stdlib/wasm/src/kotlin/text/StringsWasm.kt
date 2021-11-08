/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified offset.
 */
internal actual fun String.nativeIndexOf(ch: Char, fromIndex: Int): Int {
    for (index in fromIndex.coerceAtLeast(0)..this.lastIndex) {
        if (ch == get(index)) return index
    }
    return -1
}

/**
 * Returns the index within this string of the last occurrence of the specified character.
 */
internal actual fun String.nativeLastIndexOf(ch: Char, fromIndex: Int): Int {
    for (index in fromIndex.coerceAtMost(this.lastIndex) downTo 0) {
        if (ch == get(index)) return index
    }
    return -1
}

/**
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
internal actual fun String.nativeIndexOf(str: String, fromIndex: Int): Int {
    for (index in fromIndex.coerceAtLeast(0)..this.length) {
        if (str.regionMatchesImpl(0, this, index, str.length, false)) {
            return index
        }
    }
    return -1
}

/**
 * Returns the index within this string of the last occurrence of the specified character, starting from the specified offset.
 */
internal actual fun String.nativeLastIndexOf(str: String, fromIndex: Int): Int {
    for (index in fromIndex.coerceAtMost(this.lastIndex) downTo 0) {
        if (str.regionMatchesImpl(0, this, index, str.length, false)) {
            return index
        }
    }
    return -1
}