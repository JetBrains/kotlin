/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * Returns the index within this string of the first occurrence of the specified substring, starting from the specified offset.
 */
internal actual fun String.nativeIndexOf(str: String, fromIndex: Int): Int {
    for (index in fromIndex.coerceAtLeast(0)..(this.length - str.length)) {
        if (str.regionMatchesImpl(0, this, index, str.length, false)) {
            return index
        }
    }
    return -1
}

/**
 * Returns a string containing this char sequence repeated [n] times.
 * @throws [IllegalArgumentException] when n < 0.
 * @sample samples.text.Strings.repeat
 */
public actual fun CharSequence.repeat(n: Int): String {
    require(n >= 0) { "Count 'n' must be non-negative, but was $n." }
    if (isEmpty()) return ""
    return when (n) {
        0 -> ""
        1 -> this.toString()
        else -> {
            val sequence = this
            buildString(n * length) {
                repeat(n) {
                    append(sequence)
                }
            }
        }
    }
}

/**
 * Returns a new string with all occurrences of [oldChar] replaced with [newChar].
 */
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean = false): String {
    return buildString(length) {
        this@replace.forEach { c ->
            append(if (c.equals(oldChar, ignoreCase)) newChar else c)
        }
    }
}

/**
 * Returns a new string obtained by replacing all occurrences of the [oldValue] substring in this string
 * with the specified [newValue] string.
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

