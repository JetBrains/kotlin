/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("CollectionsKt")

package kotlin.collections

private open class ReversedListReadOnly<out T>(private val delegate: List<T>) : AbstractList<T>() {
    override val size: Int get() = delegate.size
    override fun get(index: Int): T = delegate[reverseElementIndex(index)]
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
}

private fun List<*>.reverseElementIndex(index: Int) =
    if (index in 0..lastIndex) lastIndex - index else throw IndexOutOfBoundsException("Element index $index must be in range [${0..lastIndex}].")

private fun List<*>.reversePositionIndex(index: Int) =
    if (index in 0..size) size - index else throw IndexOutOfBoundsException("Position index $index must be in range [${0..size}].")


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

