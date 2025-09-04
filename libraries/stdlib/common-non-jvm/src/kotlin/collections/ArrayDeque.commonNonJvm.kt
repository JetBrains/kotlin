/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
public actual class ArrayDeque<E> private actual constructor(
    internal actual var elementData: Array<Any?>,
) : AbstractMutableList<E>() {
    internal actual var head: Int = 0

    actual override var size: Int = 0
        internal set

    /**
     * Constructs an empty deque with specified [initialCapacity], or throws [IllegalArgumentException] if [initialCapacity] is negative.
     */
    public actual constructor(initialCapacity: Int) : this(
        when {
            initialCapacity == 0 -> EMPTY_ARRAY_OF_ANY
            initialCapacity > 0 -> arrayOfNulls(initialCapacity)
            else -> throw IllegalArgumentException("Illegal Capacity: $initialCapacity")
        }
    )

    /**
     * Constructs an empty deque.
     */
    public actual constructor() : this(EMPTY_ARRAY_OF_ANY)

    /**
     * Constructs a deque that contains the same elements as the specified [elements] collection in the same order.
     */
    public actual constructor(elements: Collection<E>) : this(if (elements.isEmpty()) EMPTY_ARRAY_OF_ANY else elements.toTypedArray()) {
        size = elementData.size
    }

    override fun isEmpty(): Boolean = commonIsEmpty()

    /**
     * Returns the first element, or throws [NoSuchElementException] if this deque is empty.
     */
    public actual fun first(): E = commonFirst()

    /**
     * Returns the first element, or `null` if this deque is empty.
     */
    public actual fun firstOrNull(): E? = commonFirstOrNull()

    /**
     * Returns the last element, or throws [NoSuchElementException] if this deque is empty.
     */
    public actual fun last(): E = commonLast()

    /**
     * Returns the last element, or `null` if this deque is empty.
     */
    public actual fun lastOrNull(): E? = commonLastOrNull()

    /**
     * Prepends the specified [element] to this deque.
     */
    public actual fun addFirst(element: E): Unit = commonAddFirst(element)

    /**
     * Appends the specified [element] to this deque.
     */
    public actual fun addLast(element: E): Unit = commonAddLast(element)

    /**
     * Removes the first element from this deque and returns that removed element, or throws [NoSuchElementException] if this deque is empty.
     */
    @IgnorableReturnValue
    public actual fun removeFirst(): E = commonRemoveFirst()

    /**
     * Removes the first element from this deque and returns that removed element, or returns `null` if this deque is empty.
     */
    @IgnorableReturnValue
    public actual fun removeFirstOrNull(): E? = commonRemoveFirstOrNull()

    /**
     * Removes the last element from this deque and returns that removed element, or throws [NoSuchElementException] if this deque is empty.
     */
    @IgnorableReturnValue
    public actual fun removeLast(): E = commonRemoveLast()

    /**
     * Removes the last element from this deque and returns that removed element, or returns `null` if this deque is empty.
     */
    @IgnorableReturnValue
    public actual fun removeLastOrNull(): E? = commonRemoveLastOrNull()

    // MutableList, MutableCollection
    @IgnorableReturnValue
    public actual override fun add(element: E): Boolean = commonAdd(element)

    public actual override fun add(index: Int, element: E): Unit = commonAdd(index, element)

    @IgnorableReturnValue
    public actual override fun addAll(elements: Collection<E>): Boolean = commonAddAll(elements)

    @IgnorableReturnValue
    public actual override fun addAll(index: Int, elements: Collection<E>): Boolean = commonAddAll(index, elements)

    public actual override fun get(index: Int): E = commonGet(index)

    @IgnorableReturnValue
    public actual override fun set(index: Int, element: E): E = commonSet(index, element)

    public actual override fun contains(element: E): Boolean = commonContains(element)

    public actual override fun indexOf(element: E): Int = commonIndexOf(element)

    public actual override fun lastIndexOf(element: E): Int = commonLastIndexOf(element)

    @IgnorableReturnValue
    public actual override fun remove(element: E): Boolean = commonRemove(element)

    @IgnorableReturnValue
    public actual override fun removeAt(index: Int): E = commonRemoveAt(index)

    @IgnorableReturnValue
    public override fun removeAll(elements: Collection<E>): Boolean = commonRemoveAll(elements)

    @IgnorableReturnValue
    public override fun retainAll(elements: Collection<E>): Boolean = commonRetainAll(elements)

    public actual override fun clear(): Unit = commonClear()

    @Suppress("NOTHING_TO_OVERRIDE", "NO_EXPLICIT_VISIBILITY_IN_API_MODE") // different visibility inherited from the base class
    override fun <T> toArray(array: Array<T>): Array<T> = commonToArray(array)

    @Suppress("NOTHING_TO_OVERRIDE", "NO_EXPLICIT_VISIBILITY_IN_API_MODE") // different visibility inherited from the base class
    override fun toArray(): Array<Any?> = commonToArray()

    override fun removeRange(fromIndex: Int, toIndex: Int): Unit = commonRemoveRange(fromIndex, toIndex)

    internal actual fun registerModification() {
        ++modCount
    }

    internal actual companion object
}
