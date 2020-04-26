/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual open class HashSet<E> : MutableSet<E> {
    actual constructor()  { TODO("Wasm stdlib: HashSet") }
    actual constructor(initialCapacity: Int) { TODO("Wasm stdlib: HashSet") }
    actual constructor(initialCapacity: Int, loadFactor: Float) { TODO("Wasm stdlib: HashSet") }
    actual constructor(elements: Collection<E>) { TODO("Wasm stdlib: HashSet") }

    // From Set

    actual override val size: Int = TODO("Wasm stdlib: HashSet")
    actual override fun isEmpty(): Boolean = TODO("Wasm stdlib: HashSet")
    actual override fun contains(element: @UnsafeVariance E): Boolean = TODO("Wasm stdlib: HashSet")
    actual override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean  = TODO("Wasm stdlib: HashSet")

    // From MutableSet

    actual override fun iterator(): MutableIterator<E> = TODO("Wasm stdlib: HashSet")
    actual override fun add(element: E): Boolean = TODO("Wasm stdlib: HashSet")
    actual override fun remove(element: E): Boolean = TODO("Wasm stdlib: HashSet")
    actual override fun addAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: HashSet")
    actual override fun removeAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: HashSet")
    actual override fun retainAll(elements: Collection<E>): Boolean = TODO("Wasm stdlib: HashSet")
    actual override fun clear() { TODO("Wasm stdlib: HashSet") }
}