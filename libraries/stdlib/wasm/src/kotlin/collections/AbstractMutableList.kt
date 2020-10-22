/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableList] interface.
 *
 * @param E the type of elements contained in the list. The list is invariant on its element type.
 */
public actual abstract class AbstractMutableList<E> : MutableList<E> {
    actual protected constructor()

    // From List

    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun contains(element: @UnsafeVariance E): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun indexOf(element: @UnsafeVariance E): Int = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun lastIndexOf(element: @UnsafeVariance E): Int = TODO("Wasm stdlib: AbstractMutableList")

    // From MutableCollection

    actual override fun iterator(): MutableIterator<E> = TODO("Wasm stdlib: AbstractMutableList")

    // From MutableList

    /**
     * Adds the specified element to the end of this list.
     *
     * @return `true` because the list is always modified as the result of this operation.
     */
    actual override fun add(element: E): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun remove(element: E): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun addAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun addAll(index: Int, elements: Collection<E>): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun removeAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun retainAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun clear() { TODO("Wasm stdlib: AbstractMutableList") }
    actual override fun listIterator(): MutableListIterator<E> = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun listIterator(index: Int): MutableListIterator<E> = TODO("Wasm stdlib: AbstractMutableList")
    actual override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = TODO("Wasm stdlib: AbstractMutableList")
}