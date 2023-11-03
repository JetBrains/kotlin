/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.utils

import com.intellij.util.SmartFMap
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.util.*
import java.util.Collections.singletonList
import java.util.Collections.singletonMap

internal inline fun <K : Any, V, R> Map<K, V>.compactMapValues(transform: (Map.Entry<K, V>) -> R): Map<K, R> =
    when (size) {
        0 -> emptyMap()
        1 -> with(entries.iterator().next()) { singletonMap(key, transform(this)) }
        2, 3 -> entries.fold(SmartFMap.emptyMap()) { acc, entry -> acc.plus(entry.key, transform(entry)) }
        else -> mapValuesTo(CommonizerMap(size), transform)
    }

internal inline fun <T, R> Collection<T>.compactMap(transform: (T) -> R): List<R> =
    when (size) {
        0 -> emptyList()
        1 -> singletonList(transform(if (this is List) this[0] else iterator().next()))
        else -> mapTo(ArrayList(size), transform)
    }

internal inline fun <T, R> Array<T>.compactMap(transform: (T) -> R): List<R> =
    when (size) {
        0 -> emptyList()
        1 -> singletonList(transform(this[0]))
        else -> mapTo(ArrayList(size), transform)
    }

internal inline fun <T, R : Any> Array<T>.compactMapNotNull(transform: (T) -> R): List<R> {
    return mapNotNullTo(ArrayList<R>(size), transform).also { it.trimToSize() }
}

internal inline fun <T, reified R : Any> Collection<T>.compactMapNotNull(transform: (T) -> R?): List<R> =
    if (isEmpty()) emptyList() else mapNotNullTo(ArrayList(size), transform).compact()

internal inline fun <T, R> Collection<T>.compactMapIndexed(transform: (index: Int, T) -> R): List<R> =
    when (size) {
        0 -> emptyList()
        1 -> singletonList(transform(0, if (this is List) this[0] else iterator().next()))
        else -> mapIndexedTo(ArrayList(size), transform)
    }

internal inline fun <reified T> List<T>.compact(): List<T> =
    when (size) {
        0 -> emptyList()
        1 -> singletonList(this[0])
        else -> when (this) {
            is java.util.ArrayList -> {
                trimToSize()
                this
            }
            else -> {
                @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
                Arrays.asList(*toTypedArray())
            }
        }
    }

internal fun <K, V> Map<K, V>.compact(): Map<K, V> =
    when (size) {
        0 -> emptyMap()
        1 -> with(entries.iterator().next()) { singletonMap(key, value) }
        2, 3 -> SmartFMap.emptyMap<K, V>().plusAll(this)
        else -> CommonizerMap(this)
    }

@Suppress("NOTHING_TO_INLINE")
internal inline fun <K : Any, V> compactMapOf(key: K, value: V): Map<K, V> =
    singletonMap(key, value)

@Suppress("NOTHING_TO_INLINE")
internal inline fun <K : Any, V> compactMapOf(key1: K, value1: V, key2: K, value2: V): Map<K, V> =
    SmartFMap.emptyMap<K, V>().plus(key1, value1).plus(key2, value2)

internal inline fun <reified T> Iterable<T?>.firstNonNull() = firstIsInstance<T>()

internal inline fun <reified T, reified K : Any> Collection<T>.foldToMap(keySelector: (T) -> K): Map<K, List<T>> {
    val result = fold(CommonizerMap<K, MutableList<T>>()) { accumulator, element ->
        accumulator.getOrPut(keySelector(element)) { ArrayList() } += element
        accumulator
    }

    return result.compactMapValues { (_, elements) -> elements.compact() }
}

internal fun Any?.isNull(): Boolean = this == null

@Suppress("NOTHING_TO_INLINE")
inline fun hashCode(value: Any?): Int = value.hashCode()

@Suppress("NOTHING_TO_INLINE")
inline fun hashCode(array: Array<*>?): Int = array?.contentHashCode() ?: 0

@Suppress("NOTHING_TO_INLINE")
inline fun Int.appendHashCode(value: Any?): Int = 31 * this + hashCode(value)

@Suppress("NOTHING_TO_INLINE")
inline fun Int.appendHashCode(array: Array<*>?): Int = 31 * this + hashCode(array)

/**
 * Allocation Free 'forEach' implementation that won't use any underlying iterator.
 * Note: This implementation therefore is unsafe when mutating the collection at the same time!!!
 */
inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (index in 0 until size) {
        action(this[index])
    }
}

/**
 * Allocation Free 'forEach' implementation that won't use any underlying iterator.
 * Note: This implementation therefore is unsafe when mutating the collection at the same time!!!
 */
inline fun <T> Array<T>.fastForEach(action: (T) -> Unit) {
    for (index in 0 until size) {
        action(this[index])
    }
}

@Suppress("unchecked_cast")
internal inline fun <T, reified R> List<T>.safeCastValues(): List<R>? {
    fastForEach { if (it !is R) return null }
    return this as List<R>
}


internal fun <T : Any> List<T>.singleDistinctValueOrNull(): T? = singleDistinctValueOrNull { it }

internal inline fun <T : Any, R> List<T>.singleDistinctValueOrNull(selector: (T) -> R): R? {
    if (isEmpty()) return null
    val value = selector(this[0])
    for (index in 1 until size) {
        if (value != selector(this[index])) return null
    }
    return value
}