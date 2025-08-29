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
public expect class ArrayDeque<E> private constructor(elementData: Array<Any?>) : AbstractMutableList<E> {
    internal var head: Int

    internal var elementData: Array<Any?>

    public override var size: Int
        internal set

    /**
     * Constructs an empty deque with specified [initialCapacity], or throws [IllegalArgumentException] if [initialCapacity] is negative.
     */
    public constructor(initialCapacity: Int)

    /**
     * Constructs an empty deque.
     */
    public constructor()

    /**
     * Constructs a deque that contains the same elements as the specified [elements] collection in the same order.
     */
    public constructor(elements: Collection<E>)

    /**
     * Returns the first element, or throws [NoSuchElementException] if this deque is empty.
     */
    public fun first(): E

    /**
     * Returns the first element, or `null` if this deque is empty.
     */
    public fun firstOrNull(): E?

    /**
     * Returns the last element, or throws [NoSuchElementException] if this deque is empty.
     */
    public fun last(): E

    /**
     * Returns the last element, or `null` if this deque is empty.
     */
    public fun lastOrNull(): E?

    /**
     * Prepends the specified [element] to this deque.
     */
    public fun addFirst(element: E)

    /**
     * Appends the specified [element] to this deque.
     */
    public fun addLast(element: E)

    /**
     * Removes the first element from this deque and returns that removed element, or throws [NoSuchElementException] if this deque is empty.
     */
    @IgnorableReturnValue
    public fun removeFirst(): E

    /**
     * Removes the first element from this deque and returns that removed element, or returns `null` if this deque is empty.
     */
    @IgnorableReturnValue
    public fun removeFirstOrNull(): E?

    /**
     * Removes the last element from this deque and returns that removed element, or throws [NoSuchElementException] if this deque is empty.
     */
    @IgnorableReturnValue
    public fun removeLast(): E

    /**
     * Removes the last element from this deque and returns that removed element, or returns `null` if this deque is empty.
     */
    @IgnorableReturnValue
    public fun removeLastOrNull(): E?

    // MutableList implementation
    override fun add(element: E): Boolean
    override fun add(index: Int, element: E)
    override fun addAll(elements: Collection<E>): Boolean
    override fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun get(index: Int): E
    override fun set(index: Int, element: E): E
    override fun contains(element: E): Boolean
    override fun indexOf(element: E): Int
    override fun lastIndexOf(element: E): Int
    override fun remove(element: E): Boolean
    override fun removeAt(index: Int): E
    override fun clear()

    internal fun registerModification()

    internal companion object
}
