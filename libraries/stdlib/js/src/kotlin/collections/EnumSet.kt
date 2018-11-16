/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual abstract class EnumSet<E : Enum<E>>(
    protected val clazz: JsClass<E>,
    protected val universe: Array<E>
) : MutableSet<E> {

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
    abstract fun addAll()
}

fun <E : Enum<E>> enumSetOf(clazz: JsClass<E>, universe: Array<E>): EnumSet<E> = if (universe.size > 32) {
    TODO() // implementation JumboEnumSet
} else {
    RegularEnumSet(clazz, universe)
}

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(): EnumSet<E> {
    return enumSetOf(E::class.js, enumValues<E>())
}

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(element: E): EnumSet<E> {
    val result = enumSetOf(E::class.js, enumValues<E>())

    result.add(element)

    return result
}

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> {
    val result = enumSetOf(E::class.js, enumValues<E>())

    result.addAll(elements)

    return result
}

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetAllOf(): EnumSet<E> {
    val result = enumSetOf(E::class.js, enumValues<E>())

    result.addAll()

    return result
}
