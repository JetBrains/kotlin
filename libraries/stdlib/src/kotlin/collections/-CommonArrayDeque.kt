/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("-CommonArrayDeque")

package kotlin.collections

import kotlin.internal.InlineOnly
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

@JvmField
internal val EMPTY_ARRAY_OF_ANY = emptyArray<Any?>()

@InlineOnly
private inline fun ArrayDeque<*>.errorNoSuchElement(): Nothing = throw NoSuchElementException("ArrayDeque is empty.")

@InlineOnly
private inline fun ArrayDeque<*>.positiveMod(index: Int): Int = if (index >= elementData.size) index - elementData.size else index

@InlineOnly
private inline fun ArrayDeque<*>.negativeMod(index: Int): Int = if (index < 0) index + elementData.size else index

@InlineOnly
private inline fun ArrayDeque<*>.internalIndex(index: Int): Int = positiveMod(head + index)

@InlineOnly
private inline fun ArrayDeque<*>.incremented(index: Int): Int = if (index == elementData.lastIndex) 0 else index + 1

@InlineOnly
private inline fun ArrayDeque<*>.decremented(index: Int): Int = if (index == 0) elementData.lastIndex else index - 1

@InlineOnly
private inline val ArrayDeque<*>.defaultMinCapacity get() = 10

@InlineOnly
private inline fun <E> ArrayDeque<E>.internalGet(internalIndex: Int): E {
    @Suppress("UNCHECKED_CAST")
    return elementData[internalIndex] as E
}

@InlineOnly
private inline fun ArrayDeque<*>.removeRangeShiftPreceding(fromIndex: Int, toIndex: Int) {
    var copyFromIndex = internalIndex(fromIndex - 1)    // upper bound of range, inclusive
    var copyToIndex = internalIndex(toIndex - 1)        // upper bound of range, inclusive
    var copyCount = fromIndex

    while (copyCount > 0) { // maximum 3 iterations
        val segmentLength = minOf(copyCount, copyFromIndex + 1, copyToIndex + 1)
        elementData.copyInto(elementData, copyToIndex - segmentLength + 1, copyFromIndex - segmentLength + 1, copyFromIndex + 1)

        copyFromIndex = negativeMod(copyFromIndex - segmentLength)
        copyToIndex = negativeMod(copyToIndex - segmentLength)
        copyCount -= segmentLength
    }
}

@InlineOnly
private inline fun ArrayDeque<*>.removeRangeShiftSucceeding(fromIndex: Int, toIndex: Int) {
    var copyFromIndex = internalIndex(toIndex) // lower bound of range, inclusive
    var copyToIndex = internalIndex(fromIndex) // lower bound of range, inclusive
    var copyCount = size - toIndex

    while (copyCount > 0) { // maximum 3 iterations
        val segmentLength = minOf(copyCount, elementData.size - copyFromIndex, elementData.size - copyToIndex)
        elementData.copyInto(elementData, copyToIndex, copyFromIndex, copyFromIndex + segmentLength)

        copyFromIndex = positiveMod(copyFromIndex + segmentLength)
        copyToIndex = positiveMod(copyToIndex + segmentLength)
        copyCount -= segmentLength
    }
}

/** If `internalFromIndex == internalToIndex`, the buffer is considered full and all elements are nullified. */
@InlineOnly
private inline fun ArrayDeque<*>.nullifyNonEmpty(internalFromIndex: Int, internalToIndex: Int) {
    if (internalFromIndex < internalToIndex) {
        elementData.fill(null, internalFromIndex, internalToIndex)
    } else {
        elementData.fill(null, internalFromIndex, elementData.size)
        elementData.fill(null, 0, internalToIndex)
    }
}

/**
 * Ensures that the capacity of this deque is at least equal to the specified [minCapacity].
 *
 * If the current capacity is less than the [minCapacity], a new backing storage is allocated with greater capacity.
 * Otherwise, this method takes no action and simply returns.
 */
internal fun ArrayDeque<*>.internalEnsureCapacity(minCapacity: Int) {
    if (minCapacity < 0) throw IllegalStateException("Deque is too big.")    // overflow
    if (minCapacity <= elementData.size) return
    if (elementData === EMPTY_ARRAY_OF_ANY) {
        elementData = arrayOfNulls(minCapacity.coerceAtLeast(defaultMinCapacity))
        return
    }

    val newCapacity = AbstractList.newCapacity(elementData.size, minCapacity)
    internalCopyElements(newCapacity)
}

internal fun ArrayDeque<*>.internalCopyElements(newCapacity: Int) {
    val newElements = arrayOfNulls<Any?>(newCapacity)
    elementData.copyInto(newElements, 0, head, elementData.size)
    elementData.copyInto(newElements, elementData.size - head, 0, head)
    head = 0
    elementData = newElements
}

internal fun <E> ArrayDeque<E>.copyCollectionElements(internalIndex: Int, elements: Collection<E>) {
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

/**
 * Applies the given [predicate] to each element of the deque
 * and filters in-place only those elements for which the predicate returned `true`.
 *
 * Returns `true` if the deque was modified as a result of the operation.
 */
@InlineOnly
internal inline fun <E> ArrayDeque<E>.filterInPlace(predicate: (E) -> Boolean): Boolean {
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
    if (modified) {
        registerModification()
        size = negativeMod(newTail - head)
    }

    return modified
}

// For testing
internal fun <T> ArrayDeque<*>.testToArray(array: Array<T>): Array<T> = commonToArray(array)
internal fun ArrayDeque<*>.testToArray(): Array<Any?> = commonToArray()
internal fun ArrayDeque<*>.testRemoveRange(fromIndex: Int, toIndex: Int) = commonRemoveRange(fromIndex, toIndex)

internal fun ArrayDeque<*>.internalStructure(structure: (head: Int, elements: Array<Any?>) -> Unit) {
    val tail = internalIndex(size)
    val head = if (isEmpty() || head < tail) head else head - elementData.size
    structure(head, commonToArray())
}

// Common implementations

@InlineOnly
internal inline fun ArrayDeque<*>.commonIsEmpty(): Boolean = size == 0

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonFirst(): E = if (isEmpty()) errorNoSuchElement() else internalGet(head)

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonFirstOrNull(): E? = if (isEmpty()) null else internalGet(head)

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonLast(): E = if (isEmpty()) errorNoSuchElement() else internalGet(internalIndex(lastIndex))

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonLastOrNull(): E? = if (isEmpty()) null else internalGet(internalIndex(lastIndex))

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonAddFirst(element: E) {
    registerModification()
    internalEnsureCapacity(size + 1)

    head = decremented(head)
    elementData[head] = element
    size += 1
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonAddLast(element: E) {
    registerModification()
    internalEnsureCapacity(size + 1)

    elementData[internalIndex(size)] = element
    size += 1
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRemoveFirst(): E {
    if (isEmpty()) errorNoSuchElement()
    registerModification()

    val element = internalGet(head)
    elementData[head] = null
    head = incremented(head)
    size -= 1
    return element
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRemoveFirstOrNull(): E? = if (isEmpty()) null else removeFirst()

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRemoveLast(): E {
    if (isEmpty()) errorNoSuchElement()
    registerModification()

    val internalLastIndex = internalIndex(lastIndex)
    val element = internalGet(internalLastIndex)
    elementData[internalLastIndex] = null
    size -= 1
    return element
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRemoveLastOrNull(): E? = if (isEmpty()) null else removeLast()

// MutableList, MutableCollection

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonAdd(element: E): Boolean {
    addLast(element)
    return true
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonAdd(index: Int, element: E) {
    AbstractList.checkPositionIndex(index, size)

    if (index == size) {
        addLast(element)
        return
    } else if (index == 0) {
        addFirst(element)
        return
    }

    registerModification()
    internalEnsureCapacity(size + 1)

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

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonAddAll(elements: Collection<E>): Boolean {
    if (elements.isEmpty()) return false

    registerModification()
    internalEnsureCapacity(size + elements.size)
    copyCollectionElements(internalIndex(size), elements)
    return true
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonAddAll(index: Int, elements: Collection<E>): Boolean {
    AbstractList.checkPositionIndex(index, size)

    if (elements.isEmpty()) {
        return false
    } else if (index == size) {
        return addAll(elements)
    }

    registerModification()
    internalEnsureCapacity(size + elements.size)

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

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonGet(index: Int): E {
    AbstractList.checkElementIndex(index, size)

    return internalGet(internalIndex(index))
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonSet(index: Int, element: E): E {
    AbstractList.checkElementIndex(index, size)

    val internalIndex = internalIndex(index)
    val oldElement = internalGet(internalIndex)
    elementData[internalIndex] = element

    return oldElement
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonContains(element: E): Boolean = indexOf(element) != -1

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonIndexOf(element: E): Int {
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

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonLastIndexOf(element: E): Int {
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

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRemove(element: E): Boolean {
    val index = indexOf(element)
    if (index == -1) return false
    removeAt(index)
    return true
}

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRemoveAt(index: Int): E {
    AbstractList.checkElementIndex(index, size)

    if (index == lastIndex) {
        return removeLast()
    } else if (index == 0) {
        return removeFirst()
    }

    registerModification()

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

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRemoveAll(elements: Collection<E>): Boolean = filterInPlace { !elements.contains(it) }

@InlineOnly
internal inline fun <E> ArrayDeque<E>.commonRetainAll(elements: Collection<E>): Boolean = filterInPlace { elements.contains(it) }

@InlineOnly
internal inline fun ArrayDeque<*>.commonClear() {
    if (isNotEmpty()) {
        registerModification()

        val tail = internalIndex(size)
        nullifyNonEmpty(head, tail)
    }
    head = 0
    size = 0
}

@InlineOnly
internal inline fun <T> ArrayDeque<*>.commonToArray(array: Array<T>): Array<T> {
    @Suppress("UNCHECKED_CAST")
    val dest = (if (array.size >= size) array else arrayOfNulls(array, size)) as Array<Any?>

    val tail = internalIndex(size)
    if (head < tail) {
        elementData.copyInto(dest, startIndex = head, endIndex = tail)
    } else if (isNotEmpty()) {
        elementData.copyInto(dest, destinationOffset = 0, startIndex = head, endIndex = elementData.size)
        elementData.copyInto(dest, destinationOffset = elementData.size - head, startIndex = 0, endIndex = tail)
    }

    @Suppress("UNCHECKED_CAST")
    return terminateCollectionToArray(size, dest) as Array<T>
}

@InlineOnly
internal inline fun ArrayDeque<*>.commonToArray(): Array<Any?> = commonToArray(arrayOfNulls<Any?>(size))

@InlineOnly
internal inline fun ArrayDeque<*>.commonRemoveRange(fromIndex: Int, toIndex: Int) {
    AbstractList.checkRangeIndexes(fromIndex, toIndex, size)

    val length = toIndex - fromIndex
    when (length) {
        0 -> return
        size -> {
            clear()
            return
        }
        1 -> {
            removeAt(fromIndex)
            return
        }
    }

    registerModification()

    if (fromIndex < size - toIndex) {
        // closer to the first element -> shift preceding elements
        removeRangeShiftPreceding(fromIndex, toIndex)

        val newHead = positiveMod(head + length)
        nullifyNonEmpty(head, newHead)
        head = newHead
    } else {
        // closer to the last element -> shift succeeding elements
        removeRangeShiftSucceeding(fromIndex, toIndex)

        val tail = internalIndex(size)
        nullifyNonEmpty(negativeMod(tail - length), tail)
    }

    size -= length
}
