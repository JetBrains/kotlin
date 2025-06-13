/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

private open class ReversedListReadOnly<out T>(private val delegate: List<T>) : AbstractList<T>() {
    override val size: Int get() = delegate.size
    override fun get(index: Int): T = delegate[reverseElementIndex(index)]

    override fun iterator(): Iterator<T> = listIterator(0)
    override fun listIterator(): ListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): ListIterator<T> = object : ListIterator<T> {
        val delegateIterator = delegate.listIterator(reversePositionIndex(index))
        override fun hasNext(): Boolean = delegateIterator.hasPrevious()
        override fun hasPrevious(): Boolean = delegateIterator.hasNext()
        override fun next(): T = delegateIterator.previous()
        override fun nextIndex(): Int = reverseIteratorIndex(delegateIterator.previousIndex())
        override fun previous(): T = delegateIterator.next()
        override fun previousIndex(): Int = reverseIteratorIndex(delegateIterator.nextIndex())
    }
}

private class ReversedList<T>(private val delegate: MutableList<T>) : AbstractMutableList<T>() {
    override val size: Int get() = delegate.size
    override fun get(index: Int): T = delegate[reverseElementIndex(index)]

    override fun clear() = delegate.clear()
    override fun removeAt(index: Int): T = delegate.removeAt(reverseElementIndex(index))

    override fun set(index: Int, element: T): T = delegate.set(reverseElementIndex(index), element)
    override fun add(index: Int, element: T) {
        delegate.add(reversePositionIndex(index), element)
    }

    override fun iterator(): MutableIterator<T> = listIterator(0)
    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> = object : MutableListIterator<T> {
        val delegateIterator = delegate.listIterator(reversePositionIndex(index))
        override fun hasNext(): Boolean = delegateIterator.hasPrevious()
        override fun hasPrevious(): Boolean = delegateIterator.hasNext()
        override fun next(): T = delegateIterator.previous()
        override fun nextIndex(): Int = reverseIteratorIndex(delegateIterator.previousIndex())
        override fun previous(): T = delegateIterator.next()
        override fun previousIndex(): Int = reverseIteratorIndex(delegateIterator.nextIndex())
        override fun add(element: T) {
            delegateIterator.add(element)
            // After an insertion previous() will return an inserted element.
            // Moving a cursor back by one element to return a correct value from next().
            val _ = delegateIterator.previous()
        }

        override fun remove() = delegateIterator.remove()
        override fun set(element: T) = delegateIterator.set(element)
    }
}

private fun List<*>.reverseElementIndex(index: Int) =
    if (index in 0..lastIndex) lastIndex - index else throw IndexOutOfBoundsException("Element index $index must be in range [${0..lastIndex}].")

private fun List<*>.reversePositionIndex(index: Int) =
    if (index in 0..size) size - index else throw IndexOutOfBoundsException("Position index $index must be in range [${0..size}].")

private fun List<*>.reverseIteratorIndex(index: Int) = lastIndex - index

/**
 * Returns a reversed read-only view of the original List.
 * All changes made in the original list will be reflected in the reversed one.
 * @sample samples.collections.ReversedViews.asReversedList
 */
public fun <T> List<T>.asReversed(): List<T> = ReversedListReadOnly(this)

/**
 * Returns a reversed mutable view of the original mutable List.
 * All changes made in the original list will be reflected in the reversed one and vice versa.
 * @sample samples.collections.ReversedViews.asReversedMutableList
 */
@kotlin.jvm.JvmName("asReversedMutable")
public fun <T> MutableList<T>.asReversed(): MutableList<T> = ReversedList(this)
