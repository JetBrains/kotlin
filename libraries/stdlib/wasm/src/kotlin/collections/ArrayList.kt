/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual open class ArrayList<E> : MutableList<E>, RandomAccess {
    actual constructor() { TODO("Wasm stdlib: ArrayList") }
    actual constructor(initialCapacity: Int) { TODO("Wasm stdlib: ArrayList") }
    actual constructor(elements: Collection<E>) { TODO("Wasm stdlib: ArrayList") }

    actual fun trimToSize() { TODO("Wasm stdlib: ArrayList") }
    actual fun ensureCapacity(minCapacity: Int) { TODO("Wasm stdlib: ArrayList") }

    // From List

    actual override val size: Int = TODO("Wasm stdlib: ArrayList")
    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun contains(element: @UnsafeVariance E): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override operator fun get(index: Int): E = TODO("Wasm stdlib: ArrayList")
    actual override fun indexOf(element: @UnsafeVariance E): Int = TODO("Wasm stdlib: ArrayList")
    actual override fun lastIndexOf(element: @UnsafeVariance E): Int = TODO("Wasm stdlib: ArrayList")

    // From MutableCollection

    actual override fun iterator(): MutableIterator<E> = TODO("Wasm stdlib: ArrayList")

    // From MutableList

    actual override fun add(element: E): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun remove(element: E): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun addAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun addAll(index: Int, elements: Collection<E>): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun removeAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun retainAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: ArrayList")
    actual override fun clear() { TODO("Wasm stdlib: ArrayList") }
    actual override operator fun set(index: Int, element: E): E = TODO("Wasm stdlib: ArrayList")
    actual override fun add(index: Int, element: E) { TODO("Wasm stdlib: ArrayList") }
    actual override fun removeAt(index: Int): E = TODO("Wasm stdlib: ArrayList")
    actual override fun listIterator(): MutableListIterator<E> = TODO("Wasm stdlib: ArrayList")
    actual override fun listIterator(index: Int): MutableListIterator<E> = TODO("Wasm stdlib: ArrayList")
    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = TODO("Wasm stdlib: ArrayList")
}