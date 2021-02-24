/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableCollection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is invariant on its element type.
 */
@SinceKotlin("1.3")
public actual abstract class AbstractMutableCollection<E> : MutableCollection<E> {
    actual protected constructor()

    actual abstract override val size: Int
    actual abstract override fun iterator(): MutableIterator<E>
    actual abstract override fun add(element: E): Boolean

    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: AbstractMutableCollection")
    actual override fun contains(element: @UnsafeVariance E): Boolean = TODO("Wasm stdlib: AbstractMutableCollection")
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = TODO("Wasm stdlib: AbstractMutableCollection")


    actual override fun addAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: AbstractMutableCollection")
    actual override fun remove(element: E): Boolean = TODO("Wasm stdlib: AbstractMutableCollection")
    actual override fun removeAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: AbstractMutableCollection")
    actual override fun retainAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: AbstractMutableCollection")
    actual override fun clear(): Unit = TODO("Wasm stdlib: AbstractMutableCollection")
}

