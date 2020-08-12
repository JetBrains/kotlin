/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import gnu.trove.THashMap
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

internal fun <T> Sequence<T>.toList(expectedCapacity: Int): List<T> {
    val result = ArrayList<T>(expectedCapacity)
    toCollection(result)
    return result
}

internal infix fun <K, V> Map<K, V>.concat(other: Map<K, V>): Map<K, V> =
    when {
        isEmpty() -> other
        other.isEmpty() -> this
        else -> THashMap<K, V>(size + other.size, 1F).apply {
            putAll(this@concat)
            putAll(other)
        }
    }

internal inline fun <reified T> Iterable<T?>.firstNonNull() = firstIsInstance<T>()

internal fun Any?.isNull(): Boolean = this == null

@Suppress("NOTHING_TO_INLINE")
inline fun hashCode(value: Any?): Int = value.hashCode()

@Suppress("NOTHING_TO_INLINE")
inline fun hashCode(array: Array<*>?): Int = array?.contentHashCode() ?: 0

@Suppress("NOTHING_TO_INLINE")
inline fun Int.appendHashCode(value: Any?): Int = 31 * this + hashCode(value)

@Suppress("NOTHING_TO_INLINE")
inline fun Int.appendHashCode(array: Array<*>?): Int = 31 * this + hashCode(array)
