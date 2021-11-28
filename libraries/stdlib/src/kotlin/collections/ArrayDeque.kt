/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

/**
 * Resizable-array implementation of the deque data structure.
 *
 * The name deque is short for "double ended queue" and is usually pronounced "deck".
 *
 * The collection provide methods for convenient access to the both ends.
 * It also implements [MutableList] interface and supports efficient get/set operations by index.
 */
@SinceKotlin("1.4")
@WasExperimental(ExperimentalStdlibApi::class)
public class ArrayDeque<E> : AbstractMutableList<E> {
    private var head: Int = 0
    private var elementData: Array<Any?>

    override var size: Int = 0
        private set

    /**
     * Constructs an empty deque with specified [initialCapacity], or throws [IllegalArgumentException] if [initialCapacity] is negative.
     */
    public constructor(initialCapacity: Int) {
        elementData = when {
            initialCapacity == 0 -> emptyElementData
            initialCapacity > 0 -> arrayOfNulls(initialCapacity)
            else -> throw IllegalArgumentException("Illegal Capacity: $initialCapacity")
        }
    }

    /**
     * Constructs an empty deque.
     */
    public constructor() {
        elementData = emptyElementData
    }

    /**
     * Constructs a deque that contains the same elements as the specified [elements] collection in the same order.
     */
    public constructor(elements: Collection<E>) {
        elementData = elements.toTypedArray()
        size = elementData.size
        if (elementData.isEmpty()) elementData = emptyElementData
    }

    /**
     * Ensures that the capacity of this deque is at least equal to the specified [minCapacity].
     *
     * If the current capacity is less than the [minCapacity], a new backing storage is allocated with greater capacity.
     * Otherwise, this method takes no action and simply returns.
     */
    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity < 0) throw IllegalStateException("Deque is too big.")    // overflow
        if (minCapacity <= elementData.size) return
        if (elementData === emptyElementData) {
            elementData = arrayOfNulls(minCapacity.coerceAtLeast(defaultMinCapacity))
            return
        }

        val newCapacity = newCapacity(elementData.size, minCapacity)
        copyElements(newCapacity)
    }

    /**
     * Creates a new array with the specified [newCapacity] size and copies elements in the [elementData] array to it.
     */
    private fun copyElements(newCapacity: Int) {
        val newElements = arrayOfNulls<Any?>(newCapacity)
        elementData.copyInto(newElements, 0, head, elementData.size)
        elementData.copyInto(newElements, elementData.size - head, 0, head)
        head = 0
        elementData = newElements
    }

    @kotlin.internal.InlineOnly
    private inline fun internalGet(internalIndex: Int): E {
        @Suppress("UNCHECKED_CAST")
        return elementData[internalIndex] as E
    }

    private fun positiveMod(index: Int): Int = if (index >= elementData.size) index - elementData.size else index

    private fun negativeMod(index: Int): Int = if (index < 0) index + elementData.size else index

    @kotlin.internal.InlineOnly
    private inline fun internalIndex(index: Int): Int = positiveMod(head + index)

    private fun incremented(index: Int): Int = if (index == elementData.lastIndex) 0 else index + 1

    private fun decremented(index: Int): Int = if (index == 0) elementData.lastIndex else index - 1

    override fun isEmpty(): Boolean = size == 0

    /**
     * Returns the first element, or throws [NoSuchElementException] if this deque is empty.
     */
    public fun first(): E = if (isEmpty()) throw NoSuchElementException("ArrayDeque is empty.") else internalGet(head)

    /**
     * Returns the first element, or `null` if this deque is empty.
     */
    public fun firstOrNull(): E? = if (isEmpty()) null else internalGet(head)

    /**
     * Returns the last element, or throws [NoSuchElementException] if this deque is empty.
     */
    public fun last(): E = if (isEmpty()) throw NoSuchElementException("ArrayDeque is empty.") else internalGet(internalIndex(lastIndex))

    /**
     * Returns the last element, or `null` if this deque is empty.
     */
    public fun lastOrNull(): E? = if (isEmpty()) null else internalGet(internalIndex(lastIndex))

    /**
     * Prepends the specified [element] to this deque.
     */
    public fun addFirst(element: E) {
        ensureCapacity(size + 1)

        head = decremented(head)
        elementData[head] = element
        size += 1
    }

    /**
     * Appends the specified [element] to this deque.
     */
    public fun addLast(element: E) {
        ensureCapacity(size + 1)

        elementData[internalIndex(size)] = element
        size += 1
    }

    /**
     * Removes the first element from this deque and returns that removed element, or throws [NoSuchElementException] if this deque is empty.
     */
    public fun removeFirst(): E {
        if (isEmpty()) throw NoSuchElementException("ArrayDeque is empty.")

        val element = internalGet(head)
        elementData[head] = null
        head = incremented(head)
        size -= 1
        return element
    }

    /**
     * Removes the first element from this deque and returns that removed element, or returns `null` if this deque is empty.
     */
    public fun removeFirstOrNull(): E? = if (isEmpty()) null else removeFirst()

    /**
     * Removes the last element from this deque and returns that removed element, or throws [NoSuchElementException] if this deque is empty.
     */
    public fun removeLast(): E {
        if (isEmpty()) throw NoSuchElementException("ArrayDeque is empty.")

        val internalLastIndex = internalIndex(lastIndex)
        val element = internalGet(internalLastIndex)
        elementData[internalLastIndex] = null
        size -= 1
        return element
    }

    /**
     * Removes the last element from this deque and returns that removed element, or returns `null` if this deque is empty.
     */
    public fun removeLastOrNull(): E? = if (isEmpty()) null else removeLast()

    // MutableList, MutableCollection
    public override fun add(element: E): Boolean {
        addLast(element)
        return true
    }

    public override fun add(index: Int, element: E) {
        AbstractList.checkPositionIndex(index, size)

        if (index == size) {
            addLast(element)
            return
        } else if (index == 0) {
            addFirst(element)
            return
        }

        ensureCapacity(size + 1)

        // Elements in circular array lay in 2 ways:
        //   1. `head` is less than `tail`:       [#, #, e1, e2, e3, #]
        //   2. `head` is greater than `tail`:    [e3, #, #, #, e1, e2]
        // where head is the index of the first element in the circular array,
        // and tail is the index following the last element.
        //
        // At this point the insertion index is not equal to head or tail.
        // Also the circular array can store at least one more element.
        //
        // Depending on where the given element must be inserted the preceding or the succeeding
        // elements will be shifted to make room for the element to be inserted.
        //
        // In case the preceding elements are shifted:
        //   * if the insertion index is greater than the head (regardless of circular array form)
        //      -> shift the preceding elements
        //   * otherwise, the circular array has (2) form and the insertion index is less than tail
        //      -> shift all elements in the back of the array
        //      -> shift preceding elements in the front of the array
        // In case the succeeding elements are shifted:
        //   * if the insertion index is less than the tail (regardless of circular array form)
        //      -> shift the succeeding elements
        //   * otherwise, the circular array has (2) form and the insertion index is greater than head
        //      -> shift all elements in the front of the array
        //      -> shift succeeding elements in the back of the array

        val internalIndex = internalIndex(index)

        if (index < (size + 1) shr 1) {
            // closer to the first element -> shift preceding elements
            val decrementedInternalIndex = decremented(internalIndex)
            val decrementedHead = decremented(head)

            if (decrementedInternalIndex >= head) {
                elementData[decrementedHead] = elementData[head]  // head can be zero
                elementData.copyInto(elementData, head, head + 1, decrementedInternalIndex + 1)
            } else { // head > tail
                elementData.copyInto(elementData, head - 1, head, elementData.size) // head can't be zero
                elementData[elementData.size - 1] = elementData[0]
                elementData.copyInto(elementData, 0, 1, decrementedInternalIndex + 1)
            }

            elementData[decrementedInternalIndex] = element
            head = decrementedHead
        } else {
            // closer to the last element -> shift succeeding elements
            val tail = internalIndex(size)

            if (internalIndex < tail) {
                elementData.copyInto(elementData, internalIndex + 1, internalIndex, tail)
            } else { // head > tail
                elementData.copyInto(elementData, 1, 0, tail)
                elementData[0] = elementData[elementData.size - 1]
                elementData.copyInto(elementData, internalIndex + 1, internalIndex, elementData.size - 1)
            }

            elementData[internalIndex] = element
        }
        size += 1
    }

    private fun copyCollectionElements(internalIndex: Int, elements: Collection<E>) {
        val iterator = elements.iterator()

        for (index in internalIndex until elementData.size) {
            if (!iterator.hasNext()) break
            elementData[index] = iterator.next()
        }
        for (index in 0 until head) {
            if (!iterator.hasNext()) break
            elementData[index] = iterator.next()
        }

        size += elements.size
    }

    public override fun addAll(elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false
        ensureCapacity(this.size + elements.size)
        copyCollectionElements(internalIndex(size), elements)
        return true
    }

    public override fun addAll(index: Int, elements: Collection<E>): Boolean {
        AbstractList.checkPositionIndex(index, size)

        if (elements.isEmpty()) {
            return false
        } else if (index == size) {
            return addAll(elements)
        }

        ensureCapacity(this.size + elements.size)

        val tail = internalIndex(size)
        val internalIndex = internalIndex(index)
        val elementsSize = elements.size

        if (index < (size + 1) shr 1) {
            // closer to the first element -> shift preceding elements

            var shiftedHead = head - elementsSize

            if (internalIndex >= head) {
                if (shiftedHead >= 0) {
                    elementData.copyInto(elementData, shiftedHead, head, internalIndex)
                } else { // head < tail, insertion leads to head >= tail
                    shiftedHead += elementData.size
                    val elementsToShift = internalIndex - head
                    val shiftToBack = elementData.size - shiftedHead

                    if (shiftToBack >= elementsToShift) {
                        elementData.copyInto(elementData, shiftedHead, head, internalIndex)
                    } else {
                        elementData.copyInto(elementData, shiftedHead, head, head + shiftToBack)
                        elementData.copyInto(elementData, 0, head + shiftToBack, internalIndex)
                    }
                }
            } else { // head > tail, internalIndex < tail
                elementData.copyInto(elementData, shiftedHead, head, elementData.size)
                if (elementsSize >= internalIndex) {
                    elementData.copyInto(elementData, elementData.size - elementsSize, 0, internalIndex)
                } else {
                    elementData.copyInto(elementData, elementData.size - elementsSize, 0, elementsSize)
                    elementData.copyInto(elementData, 0, elementsSize, internalIndex)
                }
            }
            head = shiftedHead
            copyCollectionElements(negativeMod(internalIndex - elementsSize), elements)
        } else {
            // closer to the last element -> shift succeeding elements

            val shiftedInternalIndex = internalIndex + elementsSize

            if (internalIndex < tail) {
                if (tail + elementsSize <= elementData.size) {
                    elementData.copyInto(elementData, shiftedInternalIndex, internalIndex, tail)
                } else { // head < tail, insertion leads to head >= tail
                    if (shiftedInternalIndex >= elementData.size) {
                        elementData.copyInto(elementData, shiftedInternalIndex - elementData.size, internalIndex, tail)
                    } else {
                        val shiftToFront = tail + elementsSize - elementData.size
                        elementData.copyInto(elementData, 0, tail - shiftToFront, tail)
                        elementData.copyInto(elementData, shiftedInternalIndex, internalIndex, tail - shiftToFront)
                    }
                }
            } else { // head > tail, internalIndex > head
                elementData.copyInto(elementData, elementsSize, 0, tail)
                if (shiftedInternalIndex >= elementData.size) {
                    elementData.copyInto(elementData, shiftedInternalIndex - elementData.size, internalIndex, elementData.size)
                } else {
                    elementData.copyInto(elementData, 0, elementData.size - elementsSize, elementData.size)
                    elementData.copyInto(elementData, shiftedInternalIndex, internalIndex, elementData.size - elementsSize)
                }
            }
            copyCollectionElements(internalIndex, elements)
        }

        return true
    }

    public override fun get(index: Int): E {
        AbstractList.checkElementIndex(index, size)

        return internalGet(internalIndex(index))
    }

    public override fun set(index: Int, element: E): E {
        AbstractList.checkElementIndex(index, size)

        val internalIndex = internalIndex(index)
        val oldElement = internalGet(internalIndex)
        elementData[internalIndex] = element

        return oldElement
    }

    public override fun contains(element: E): Boolean = indexOf(element) != -1

    public override fun indexOf(element: E): Int {
        val tail = internalIndex(size)

        if (head < tail) {
            for (index in head until tail) {
                if (element == elementData[index]) return index - head
            }
        } else if (head >= tail) {
            for (index in head until elementData.size) {
                if (element == elementData[index]) return index - head
            }
            for (index in 0 until tail) {
                if (element == elementData[index]) return index + elementData.size - head
            }
        }

        return -1
    }

    public override fun lastIndexOf(element: E): Int {
        val tail = internalIndex(size)

        if (head < tail) {
            for (index in tail - 1 downTo head) {
                if (element == elementData[index]) return index - head
            }
        } else if (head > tail) {
            for (index in tail - 1 downTo 0) {
                if (element == elementData[index]) return index + elementData.size - head
            }
            for (index in elementData.lastIndex downTo head) {
                if (element == elementData[index]) return index - head
            }
        }

        return -1
    }

    public override fun remove(element: E): Boolean {
        val index = indexOf(element)
        if (index == -1) return false
        removeAt(index)
        return true
    }

    public override fun removeAt(index: Int): E {
        AbstractList.checkElementIndex(index, size)

        if (index == lastIndex) {
            return removeLast()
        } else if (index == 0) {
            return removeFirst()
        }

        val internalIndex = internalIndex(index)
        val element = internalGet(internalIndex)

        if (index < size shr 1) {
            // closer to the first element -> shift preceding elements
            if (internalIndex >= head) {
                elementData.copyInto(elementData, head + 1, head, internalIndex)
            } else { // head > tail, internalIndex < head
                elementData.copyInto(elementData, 1, 0, internalIndex)
                elementData[0] = elementData[elementData.size - 1]
                elementData.copyInto(elementData, head + 1, head, elementData.size - 1)
            }

            elementData[head] = null
            head = incremented(head)
        } else {
            // closer to the last element -> shift succeeding elements
            val internalLastIndex = internalIndex(lastIndex)

            if (internalIndex <= internalLastIndex) {
                elementData.copyInto(elementData, internalIndex, internalIndex + 1, internalLastIndex + 1)
            } else { // head > tail, internalIndex > head
                elementData.copyInto(elementData, internalIndex, internalIndex + 1, elementData.size)
                elementData[elementData.size - 1] = elementData[0]
                elementData.copyInto(elementData, 0, 1, internalLastIndex + 1)
            }

            elementData[internalLastIndex] = null
        }
        size -= 1

        return element
    }

    public override fun removeAll(elements: Collection<E>): Boolean = filterInPlace { !elements.contains(it) }

    public override fun retainAll(elements: Collection<E>): Boolean = filterInPlace { elements.contains(it) }

    private inline fun filterInPlace(predicate: (E) -> Boolean): Boolean {
        if (this.isEmpty() || elementData.isEmpty())
            return false

        val tail = internalIndex(size)
        var newTail = head
        var modified = false

        if (head < tail) {
            for (index in head until tail) {
                val element = elementData[index]

                @Suppress("UNCHECKED_CAST")
                if (predicate(element as E))
                    elementData[newTail++] = element
                else
                    modified = true
            }

            elementData.fill(null, newTail, tail)

        } else {
            for (index in head until elementData.size) {
                val element = elementData[index]
                elementData[index] = null

                @Suppress("UNCHECKED_CAST")
                if (predicate(element as E))
                    elementData[newTail++] = element
                else
                    modified = true
            }

            newTail = positiveMod(newTail)

            for (index in 0 until tail) {
                val element = elementData[index]
                elementData[index] = null

                @Suppress("UNCHECKED_CAST")
                if (predicate(element as E)) {
                    elementData[newTail] = element
                    newTail = incremented(newTail)
                } else {
                    modified = true
                }
            }
        }
        if (modified)
            size = negativeMod(newTail - head)

        return modified
    }

    public override fun clear() {
        val tail = internalIndex(size)
        if (head < tail) {
            elementData.fill(null, head, tail)
        } else if (isNotEmpty()) {
            elementData.fill(null, head, elementData.size)
            elementData.fill(null, 0, tail)
        }
        head = 0
        size = 0
    }

    @Suppress("NOTHING_TO_OVERRIDE")
    override fun <T> toArray(array: Array<T>): Array<T> {
        @Suppress("UNCHECKED_CAST")
        val dest = (if (array.size >= size) array else arrayOfNulls(array, size)) as Array<Any?>

        val tail = internalIndex(size)
        if (head < tail) {
            elementData.copyInto(dest, startIndex = head, endIndex = tail)
        } else if (isNotEmpty()) {
            elementData.copyInto(dest, destinationOffset = 0, startIndex = head, endIndex = elementData.size)
            elementData.copyInto(dest, destinationOffset = elementData.size - head, startIndex = 0, endIndex = tail)
        }
        if (dest.size > size) {
            dest[size] = null // null-terminate
        }

        @Suppress("UNCHECKED_CAST")
        return dest as Array<T>
    }

    @Suppress("NOTHING_TO_OVERRIDE")
    override fun toArray(): Array<Any?> {
        return toArray(arrayOfNulls<Any?>(size))
    }

    // for testing
    internal fun <T> testToArray(array: Array<T>): Array<T> = toArray(array)
    internal fun testToArray(): Array<Any?> = toArray()

    internal companion object {
        private val emptyElementData = emptyArray<Any?>()
        private const val maxArraySize = Int.MAX_VALUE - 8
        private const val defaultMinCapacity = 10

        internal fun newCapacity(oldCapacity: Int, minCapacity: Int): Int {
            // overflow-conscious
            var newCapacity = oldCapacity + (oldCapacity shr 1)
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity
            if (newCapacity - maxArraySize > 0)
                newCapacity = if (minCapacity > maxArraySize) Int.MAX_VALUE else maxArraySize
            return newCapacity
        }
    }

    // For testing only
    internal fun internalStructure(structure: (head: Int, elements: Array<Any?>) -> Unit) {
        val tail = internalIndex(size)
        val head = if (isEmpty() || head < tail) head else head - elementData.size
        structure(head, toArray())
    }
}