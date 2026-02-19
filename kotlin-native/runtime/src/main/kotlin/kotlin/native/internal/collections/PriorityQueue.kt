/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.collections

internal class PriorityQueue<T>(initialCapacity: Int, private val comparator: Comparator<T>) {

    constructor(comparator: Comparator<T>) : this(DEFAULT_INITIAL_CAPACITY, comparator)

    companion object {
        private const val DEFAULT_INITIAL_CAPACITY = 11

        fun <T : Comparable<T>> minimal(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY): PriorityQueue<T> =
                PriorityQueue(initialCapacity) { a, b -> a.compareTo(b) }

        fun <T : Comparable<T>> maximal(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY): PriorityQueue<T> =
                PriorityQueue(initialCapacity) { a, b -> b.compareTo(a) }
    }

    private val elements = ArrayList<T>(initialCapacity)

    val unorderedElements: List<T>
        get() = elements.toList()

    val size: Int
        get() = elements.size

    fun isEmpty(): Boolean = elements.isEmpty()

    fun contains(element: T): Boolean = elements.contains(element)

    fun containsAll(elements: Collection<T>): Boolean = this.elements.containsAll(elements)

    fun add(element: T): Boolean {
        elements.add(element)
        siftUp(elements.size - 1)
        return true
    }

    fun firstOrNull(): T? {
        return if (isEmpty()) null else elements[0]
    }

    fun first(): T {
        return firstOrNull() ?: throw NoSuchElementException("PriorityQueue is empty")
    }

    fun removeFirstOrNull(): T? {
        if (isEmpty()) return null
        return removeAt(0)
    }

    fun removeFirst(): T = removeFirstOrNull() ?: throw NoSuchElementException("PriorityQueue is empty")

    fun clear() {
        elements.clear()
    }

    fun remove(element: T): Boolean {
        val index = elements.indexOf(element)
        if (index == -1) return false
        removeAt(index)
        return true
    }

    @IgnorableReturnValue
    private fun removeAt(index: Int): T {
        val removedElement = elements[index]
        if (index == elements.size - 1) {
            elements.removeAt(index)
        } else {
            elements[index] = elements.removeAt(elements.size - 1)
            siftDown(index)
        }
        return removedElement
    }

    private fun leftChild(index: Int): Int = 2 * index + 1
    private fun rightChild(index: Int): Int = leftChild(index) + 1
    private fun parent(index: Int): Int = (index - 1) / 2

    private operator fun T.compareTo(other: T): Int = comparator.compare(this, other)

    private fun siftUp(start: Int) {
        val startElement = elements[start]
        var child = start
        while (child > 0) {
            val parent = parent(child)
            if (startElement >= elements[parent]) break
            elements[child] = elements[parent]
            child = parent
        }
        elements[child] = startElement
    }

    private fun siftDown(start: Int) {
        val startElement = elements[start]
        var parent = start
        val firstLeaf = elements.size / 2
        while (parent < firstLeaf) {
            val left = leftChild(parent)
            val right = rightChild(parent)

            val leastChild = if (right < elements.size && elements[right] < elements[left]) {
                right
            } else {
                left
            }

            if (startElement <= elements[leastChild]) break
            elements[parent] = elements[leastChild]
            parent = leastChild
        }
        elements[parent] = startElement
    }
}
