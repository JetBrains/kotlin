/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.util.*

internal fun <T> Collection<T>.toIdentitySet(): Set<T> =
    Collections.newSetFromMap(IdentityHashMap<T, Boolean>()).apply { addAll(this@toIdentitySet) }

internal class FailOnDuplicatesSet<E : Any> : Set<E> {
    private val uniqueElements: MutableSet<E> = hashSetOf()

    operator fun plusAssign(element: E) {
        assertTrue(uniqueElements.add(element)) { "An attempt to add already existing element: $element" }
    }

    override val size get() = uniqueElements.size
    override fun isEmpty() = uniqueElements.isEmpty()
    override fun contains(element: E) = element in uniqueElements
    override fun containsAll(elements: Collection<E>) = uniqueElements.containsAll(elements)
    override fun iterator(): Iterator<E> = uniqueElements.iterator()
    override fun equals(other: Any?) = (other as? FailOnDuplicatesSet<*>)?.uniqueElements == uniqueElements
    override fun hashCode() = uniqueElements.hashCode()
}

internal inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> {
    if (this is Collection && isEmpty()) return emptySet()

    val result = hashSetOf<R>()
    mapTo(result, transform)
    return result
}

internal inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> {
    if (this is Collection && isEmpty()) return emptySet()

    val result = hashSetOf<R>()
    mapNotNullTo(result, transform)
    return result
}

internal inline fun <T, R : Any> Array<out T>.mapNotNullToSet(transform: (T) -> R?): Set<R> {
    if (isEmpty()) return emptySet()

    val result = hashSetOf<R>()
    mapNotNullTo(result, transform)
    return result
}

internal inline fun <T, R> Iterable<T>.flatMapToSet(transform: (T) -> Iterable<R>): Set<R> {
    if (this is Collection && isEmpty()) return emptySet()

    val result = hashSetOf<R>()
    flatMapTo(result, transform)
    return result
}
