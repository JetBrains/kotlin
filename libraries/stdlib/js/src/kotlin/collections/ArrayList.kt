/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Provides a [MutableList] implementation, which uses a resizable array as its backing storage.
 *
 * This implementation doesn't provide a way to manage capacity, as backing JS array is resizeable itself.
 * There is no speed advantage to pre-allocating array sizes in JavaScript, so this implementation does not include any of the
 * capacity and "growth increment" concepts.
 */
public actual open class ArrayList<E> internal constructor(private var array: Array<Any?>) : AbstractMutableList<E>(), MutableList<E>, RandomAccess {
    private var isReadOnly: Boolean = false

    /**
     * Creates an empty [ArrayList].
     */
    public actual constructor() : this(emptyArray()) {}

    /**
     * Creates an empty [ArrayList].
     * @param initialCapacity initial capacity (ignored)
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual constructor(initialCapacity: Int = 0) : this(emptyArray()) {}

    /**
     * Creates an [ArrayList] filled from the [elements] collection.
     */
    public actual constructor(elements: Collection<E>) : this(elements.toTypedArray<Any?>()) {}

    @PublishedApi
    internal fun build(): List<E> {
        checkIsMutable()
        isReadOnly = true
        return this
    }

    /** Does nothing in this ArrayList implementation. */
    public actual fun trimToSize() {}

    /** Does nothing in this ArrayList implementation. */
    public actual fun ensureCapacity(minCapacity: Int) {}

    actual override val size: Int get() = array.size
    @Suppress("UNCHECKED_CAST")
    actual override fun get(index: Int): E = array[rangeCheck(index)] as E
    actual override fun set(index: Int, element: E): E {
        checkIsMutable()
        rangeCheck(index)
        @Suppress("UNCHECKED_CAST")
        return array[index].apply { array[index] = element } as E
    }

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

    actual override fun addAll(elements: Collection<E>): Boolean {
        checkIsMutable()
        if (elements.isEmpty()) return false

        array += elements.toTypedArray<Any?>()
        modCount++
        return true
    }

    actual override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkIsMutable()
        insertionRangeCheck(index)

        if (index == size) return addAll(elements)
        if (elements.isEmpty()) return false
        when (index) {
            size -> return addAll(elements)
            0 -> array = elements.toTypedArray<Any?>() + array
            else -> array = array.copyOfRange(0, index).asDynamic().concat(elements.toTypedArray<Any?>(), array.copyOfRange(index, size))
        }

        modCount++
        return true
    }

    actual override fun removeAt(index: Int): E {
        checkIsMutable()
        rangeCheck(index)
        modCount++
        return if (index == lastIndex)
            array.asDynamic().pop()
        else
            array.asDynamic().splice(index, 1)[0]
    }

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

    override fun toString() = arrayToString(array)

    @Suppress("UNCHECKED_CAST")
    override fun <T> toArray(array: Array<T>): Array<T> {
        if (array.size < size) {
            return toArray() as Array<T>
        }

        (this.array as Array<T>).copyInto(array)

        if (array.size > size) {
            array[size] = null as T // null-terminate
        }

        return array
    }

    override fun toArray(): Array<Any?> {
        return js("[]").slice.call(array)
    }


    internal override fun checkIsMutable() {
        if (isReadOnly) throw UnsupportedOperationException()
    }

    private fun rangeCheck(index: Int) = index.apply {
        AbstractList.checkElementIndex(index, size)
    }

    private fun insertionRangeCheck(index: Int) = index.apply {
        AbstractList.checkPositionIndex(index, size)
    }
}