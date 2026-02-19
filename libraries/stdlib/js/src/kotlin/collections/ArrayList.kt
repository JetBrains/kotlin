/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JsFileName("ArrayListJs")

package kotlin.collections

import kotlin.js.collections.JsArray

/**
 * A dynamic array implementation of [MutableList].
 *
 * This class stores elements using a native JavaScript array as its backing storage.
 * It fully implements the [MutableList] contract, providing all standard list
 * operations including indexed access, iteration, and modification. As an implementation of
 * [RandomAccess], it provides fast indexed access to elements.
 *
 * ## JS-specific implementation notes
 *
 * On the JS target, this implementation uses a native JavaScript array as its backing storage.
 * JavaScript arrays are inherently dynamic and manage their own capacity automatically.
 * Therefore, capacity management methods like [ensureCapacity] and [trimToSize] have no effect.
 * There is no performance advantage to pre-allocating array sizes in JavaScript.
 *
 * ## Thread safety
 *
 * [ArrayList] is not thread-safe. If multiple threads access an instance concurrently and at least
 * one thread modifies it, external synchronization is required.
 *
 * @param E the type of elements contained in the list.
 */
public actual open class ArrayList<E> internal constructor(private var array: Array<Any?>) : AbstractMutableList<E>(), MutableList<E>, RandomAccess {
    private companion object {
        private val Empty = ArrayList<Nothing>(0).also { it.isReadOnly = true }
    }

    private var isReadOnly: Boolean = false

    /**
     * Creates a new empty [ArrayList].
     */
    public actual constructor() : this(emptyArray()) {}

    /**
     * Creates a new empty [ArrayList] with the specified initial capacity.
     *
     * Capacity is the maximum number of elements the list is able to store in current backing storage.
     * When the list gets full and a new element can't be added, its capacity is expanded,
     * which usually leads to creation of a bigger backing storage.
     *
     * @param initialCapacity the initial capacity of the created list.
     *   Note that the argument is just a hint for the implementation and can be ignored.
     *
     * @throws IllegalArgumentException if [initialCapacity] is negative.
     */
    public actual constructor(initialCapacity: Int) : this(emptyArray()) {
        require(initialCapacity >= 0) { "Negative initial capacity: $initialCapacity" }
    }

    /**
     * Creates a new [ArrayList] filled with the elements of the specified collection.
     *
     * The iteration order of elements in the created list is the same as in the specified collection.
     */
    public actual constructor(elements: Collection<E>) : this(elements.toTypedArray<Any?>()) {}

    @PublishedApi
    internal fun build(): List<E> {
        checkIsMutable()
        isReadOnly = true
        return if (size > 0) this else Empty
    }

    /** Does nothing in this ArrayList implementation. */
    public actual fun trimToSize() {}

    /** Does nothing in this ArrayList implementation. */
    public actual fun ensureCapacity(minCapacity: Int) {}

    actual override val size: Int get() = array.size
    @Suppress("UNCHECKED_CAST")
    actual override fun get(index: Int): E = array[rangeCheck(index)] as E
    @IgnorableReturnValue
    actual override fun set(index: Int, element: E): E {
        checkIsMutable()
        rangeCheck(index)
        @Suppress("UNCHECKED_CAST")
        return array[index].apply { array[index] = element } as E
    }

    @IgnorableReturnValue
    actual override fun add(element: E): Boolean {
        checkIsMutable()
        array.asDynamic().push(element)
        modCount++
        return true
    }

    actual override fun add(index: Int, element: E): Unit {
        checkIsMutable()
        array.asDynamic().splice(insertionRangeCheck(index), 0, element)
        modCount++
    }

    private fun increaseLength(amount: Int): Int {
        val previous = size
        array.asDynamic().length = size + amount
        return previous
    }

    @IgnorableReturnValue
    actual override fun addAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        if (elements.isEmpty()) return false

        val offset = increaseLength(elements.size)
        elements.forEachIndexed { index, element ->
            array[offset + index] = element
        }
        modCount++
        return true
    }

    @IgnorableReturnValue
    actual override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkIsMutable()
        insertionRangeCheck(index)

        if (index == size) return addAll(elements)
        if (elements.isEmpty()) return false

        val tail = array.asDynamic().splice(index).unsafeCast<Array<E>>()
        addAll(elements)

        val offset = increaseLength(tail.size)
        repeat(tail.size) { tailIndex ->
            array[offset + tailIndex] = tail[tailIndex]
        }

        modCount++
        return true
    }

    @IgnorableReturnValue
    actual override fun removeAt(index: Int): E {
        checkIsMutable()
        rangeCheck(index)
        modCount++
        return if (index == lastIndex)
            array.asDynamic().pop()
        else
            array.asDynamic().splice(index, 1)[0]
    }

    @IgnorableReturnValue
    actual override fun remove(element: E): Boolean {
        checkIsMutable()
        for (index in array.indices) {
            if (array[index] == element) {
                array.asDynamic().splice(index, 1)
                modCount++
                return true
            }
        }
        return false
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        checkIsMutable()
        modCount++
        array.asDynamic().splice(fromIndex, toIndex - fromIndex)
    }

    actual override fun clear() {
        checkIsMutable()
        array = emptyArray()
        modCount++
    }


    actual override fun indexOf(element: E): Int = array.indexOf(element)

    actual override fun lastIndexOf(element: E): Int = array.lastIndexOf(element)

    override fun toString(): String = arrayToString(array)

    @Suppress("UNCHECKED_CAST")
    override fun <T> toArray(array: Array<T>): Array<T> {
        if (array.size < size) {
            return toArray() as Array<T>
        }

        (this.array as Array<T>).copyInto(array)

        return terminateCollectionToArray(size, array)
    }

    override fun toArray(): Array<Any?> {
        return js("[]").slice.call(array)
    }

    @ExperimentalJsExport
    @ExperimentalJsCollectionsApi
    @SinceKotlin("2.0")
    override fun asJsArrayView(): JsArray<E> = array.unsafeCast<JsArray<E>>()

    internal override fun checkIsMutable() {
        if (isReadOnly) throw UnsupportedOperationException()
    }

    @IgnorableReturnValue
    private fun rangeCheck(index: Int) = index.apply {
        AbstractList.checkElementIndex(index, size)
    }

    @IgnorableReturnValue
    private fun insertionRangeCheck(index: Int) = index.apply {
        AbstractList.checkPositionIndex(index, size)
    }
}
