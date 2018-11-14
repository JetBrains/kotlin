package kotlin.collections

import kotlin.reflect.KClass

actual abstract class EnumSet<E : Enum<E>> protected actual constructor(clazz: KClass<E>, vararg universe: E) : MutableSet<E> {
    actual abstract override val size: Int
    actual abstract override fun isEmpty(): Boolean
    actual abstract override fun contains(element: @UnsafeVariance E): Boolean
    actual abstract override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    actual abstract override fun iterator(): MutableIterator<E>
    actual abstract override fun add(element: E): Boolean
    actual abstract override fun remove(element: E): Boolean
    actual abstract override fun addAll(elements: Collection<E>): Boolean
    actual abstract override fun removeAll(elements: Collection<E>): Boolean
    actual abstract override fun retainAll(elements: Collection<E>): Boolean
    actual abstract override fun clear()
}

@kotlin.internal.InlineOnly
public actual inline fun <reified T : Enum<T>> enumSetOf(): EnumSet<T> {
    TODO()
}

@kotlin.internal.InlineOnly
public actual inline fun <reified T : Enum<T>> enumSetOf(vararg elements: T): EnumSet<T> {
    TODO()
}

@kotlin.internal.InlineOnly
public actual inline fun <reified T : Enum<T>> enumSetAllOf(): EnumSet<T> {
    TODO()
}
