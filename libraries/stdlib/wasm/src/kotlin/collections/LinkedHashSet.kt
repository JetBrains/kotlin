/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual open class LinkedHashSet<E> : MutableSet<E> {
    actual constructor() { TODO("Wasm stdlib: LinkedHashSet") }
    actual constructor(initialCapacity: Int) { TODO("Wasm stdlib: LinkedHashSet") }
    actual constructor(initialCapacity: Int, loadFactor: Float) { TODO("Wasm stdlib: LinkedHashSet") }
    actual constructor(elements: Collection<E>) { TODO("Wasm stdlib: LinkedHashSet") }

    // From Set

    actual override val size: Int = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun contains(element: @UnsafeVariance E): Boolean = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = TODO("Wasm stdlib: LinkedHashSet")

    // From MutableSet

    actual override fun iterator(): MutableIterator<E> = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun add(element: E): Boolean = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun remove(element: E): Boolean = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun addAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun removeAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun retainAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: LinkedHashSet")
    actual override fun clear() { TODO("Wasm stdlib: LinkedHashSet") }
}