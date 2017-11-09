/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.collections

/**
 * Provides a [MutableList] implementation, which uses a resizable array as its backing storage.
 *
 * This implementation doesn't provide a way to manage capacity, as backing JS array is resizeable itself.
 * There is no speed advantage to pre-allocating array sizes in JavaScript, so this implementation does not include any of the
 * capacity and "growth increment" concepts.
 */
public open class ArrayList<E> internal constructor(private var array: Array<Any?>) : AbstractMutableList<E>(), RandomAccess {

    /**
     * Creates an empty [ArrayList].
     * @param capacity initial capacity (ignored)
     */
    public constructor(@Suppress("UNUSED_PARAMETER") capacity: Int = 0) : this(emptyArray()) {}
    /**
     * Creates an [ArrayList] filled from the [elements] collection.
     */
    public constructor(elements: Collection<E>) : this(elements.toTypedArray<Any?>()) {}

    /** Does nothing in this ArrayList implementation. */
    public fun trimToSize() {}
    /** Does nothing in this ArrayList implementation. */
    public fun ensureCapacity(@Suppress("UNUSED_PARAMETER") minCapacity: Int) {}

    override val size: Int get() = array.size
    override fun get(index: Int): E = array[rangeCheck(index)] as E
    override fun set(index: Int, element: E): E {
        rangeCheck(index)
        return array[index].apply { array[index] = element } as E
    }

    override fun add(element: E): Boolean {
        array.asDynamic().push(element)
        modCount++
        return true
    }

    override fun add(index: Int, element: E): Unit {
        array.asDynamic().splice(insertionRangeCheck(index), 0, element)
        modCount++
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false

        array += elements.toTypedArray<Any?>()
        modCount++
        return true
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
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

    override fun removeAt(index: Int): E {
        rangeCheck(index)
        modCount++
        return if (index == lastIndex)
            array.asDynamic().pop()
        else
            array.asDynamic().splice(index, 1)[0]
    }

    override fun remove(element: E): Boolean {
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
        modCount++
        array.asDynamic().splice(fromIndex, toIndex - fromIndex)
    }

    override fun clear() {
        array = emptyArray()
        modCount++
    }


    override fun indexOf(element: E): Int = array.indexOf(element)

    override fun lastIndexOf(element: E): Int = array.lastIndexOf(element)

    override fun toString() = arrayToString(array)
    override fun toArray(): Array<Any?> = array.copyOf()


    private fun rangeCheck(index: Int) = index.apply {
        AbstractList.checkElementIndex(index, size)
    }

    private fun insertionRangeCheck(index: Int) = index.apply {
        AbstractList.checkPositionIndex(index, size)
    }
}