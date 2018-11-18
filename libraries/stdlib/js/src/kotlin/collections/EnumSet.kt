/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

actual abstract class EnumSet<E : Enum<E>>(
    protected val type: JsClass<E>,
    protected val universe: Array<E>
) : AbstractMutableSet<E>(), MutableSet<E> {

    actual abstract override val size: Int
    actual abstract override fun isEmpty(): Boolean
    actual abstract override fun contains(element: @UnsafeVariance E): Boolean
    actual abstract override fun iterator(): MutableIterator<E>
    actual abstract override fun add(element: E): Boolean
    actual abstract override fun remove(element: E): Boolean
    actual abstract override fun clear()

    /** Fill up itself and return itself */
    internal abstract fun filledUp(): EnumSet<E>
}

fun <E : Enum<E>> enumSetOf(type: JsClass<E>, universe: Array<E>): EnumSet<E> = if (universe.size > Int.SIZE_BITS) {
    JumboEnumSet(type, universe)
} else {
    RegularEnumSet(type, universe)
}

fun <E : Enum<E>> enumSetAllOf(type: JsClass<E>, universe: Array<E>): EnumSet<E> = if (universe.size > Int.SIZE_BITS) {
    JumboEnumSet(type, universe).filledUp()
} else {
    RegularEnumSet(type, universe).filledUp()
}

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(): EnumSet<E> = enumSetOf(E::class.js, enumValues<E>())

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(element: E): EnumSet<E> {
    val result = enumSetOf(E::class.js, enumValues<E>())

    result.add(element)

    return result
}

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> =
    elements.toCollection(enumSetOf(E::class.js, enumValues<E>()))

@kotlin.internal.InlineOnly
public actual inline fun <reified E : Enum<E>> enumSetAllOf(): EnumSet<E> = enumSetAllOf(E::class.js, enumValues<E>())
