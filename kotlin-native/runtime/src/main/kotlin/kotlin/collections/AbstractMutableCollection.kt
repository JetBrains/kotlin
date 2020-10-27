/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.collections

/**
 * Provides a skeletal implementation of the [MutableCollection] interface.
 *
 * @param E the type of elements contained in the collection. The collection is invariant in its element type.
 */
public actual abstract class AbstractMutableCollection<E> protected actual constructor(): MutableCollection<E>, AbstractCollection<E>() {

    // Bulk Modification Operations
    /**
     * Adds all of the elements of the specified collection to this collection.
     *
     * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
     */
    actual override public fun addAll(elements: Collection<E>): Boolean {
        var changed = false
        for (v in elements) {
            if (add(v)) changed = true
        }
        return changed
    }

    /**
     * Removes a single instance of the specified element from this
     * collection, if it is present.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    actual override fun remove(element: E): Boolean {
        val it = iterator()
        while (it.hasNext()) {
            if (it.next() == element) {
                it.remove()
                return true
            }
        }
        return false
    }

    /**
     * Removes all of this collection's elements that are also contained in the specified collection.
     *
     * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
     */
    actual override public fun removeAll(elements: Collection<E>): Boolean = (this as MutableIterable<E>).removeAll { it in elements }

    /**
     * Retains only the elements in this collection that are contained in the specified collection.
     *
     * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
     */
    actual override public fun retainAll(elements: Collection<E>): Boolean = (this as MutableIterable<E>).retainAll { it in elements }

    /**
     * Removes all elements from this collection.
     */
    actual override fun clear(): Unit {
        val it = iterator()
        while (it.hasNext()) {
            it.next()
            it.remove()
        }
    }
}
