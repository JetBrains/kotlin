/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsFileName("AbstractMutableCollectionJs")

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableCollection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is invariant in its element type.
 */
@AllowDifferentMembersInActual // New 'checkIsMutable', 'toJSON', etc. members are added compared to the expect declaration
public actual abstract class AbstractMutableCollection<E> protected actual constructor() : AbstractCollection<E>(), MutableCollection<E> {

    actual abstract override fun add(element: E): Boolean

    actual override fun remove(element: E): Boolean {
        checkIsMutable()
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == element) {
                iterator.remove()
                return true
            }
        }
        return false
    }

    actual override fun addAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        var modified = false
        for (element in elements) {
            if (add(element)) modified = true
        }
        return modified
    }

    actual override fun removeAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return (this as MutableIterable<E>).removeAll { it in elements }
    }

    actual override fun retainAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        return (this as MutableIterable<E>).removeAll { it !in elements }
    }

    actual override fun clear(): Unit {
        checkIsMutable()
        val iterator = this.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    @Deprecated("Provided so that subclasses inherit this function", level = DeprecationLevel.HIDDEN)
    @JsName("toJSON")
    protected fun toJSON(): Any = this.toArray()


    /**
     * This method is called every time when a mutating method is called on this mutable collection.
     * Mutable collections that are built (frozen) must throw `UnsupportedOperationException`.
     */
    internal open fun checkIsMutable(): Unit { }
}

