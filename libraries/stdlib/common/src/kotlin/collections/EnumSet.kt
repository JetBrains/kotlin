/*
 * Copyright in here? call @mirromutth on github
 */

package kotlin.collections

import kotlin.reflect.KClass

expect abstract class EnumSet<E : Enum<E>> protected constructor(clazz: KClass<E>, vararg universe: E) : MutableSet<E> {

    // From Set

    abstract override val size: Int
    abstract override fun isEmpty(): Boolean
    abstract override fun contains(element: @UnsafeVariance E): Boolean
    abstract override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean

    // From MutableSet

    abstract override fun iterator(): MutableIterator<E>
    abstract override fun add(element: E): Boolean
    abstract override fun remove(element: E): Boolean
    abstract override fun addAll(elements: Collection<E>): Boolean
    abstract override fun removeAll(elements: Collection<E>): Boolean
    abstract override fun retainAll(elements: Collection<E>): Boolean
    abstract override fun clear()
}
