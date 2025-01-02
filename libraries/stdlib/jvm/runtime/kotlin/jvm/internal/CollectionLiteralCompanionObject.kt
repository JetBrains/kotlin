/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

internal object ListCompanionObject {
    public /*operator*/ fun <T> of(vararg elements: T): List<T> = if (elements.size > 0) elements.asList() else emptyList()
    public /*operator*/ fun <T> of(element: T): List<T> = java.util.Collections.singletonList(element)
    public /*operator*/ fun <T> of(): List<T> = emptyList()
}

internal object MutableListCompanionObject {
    public /*operator*/ fun <T> of(vararg elements: T): MutableList<T> =
        if (elements.size == 0) ArrayList() else ArrayList(ArrayAsCollection(elements, isVarargs = true))
    public /*operator*/ fun <T> of(element: T): MutableList<T> {
        val result = ArrayList<T>(1)
        result.add(element)
        return result
    }
    public /*operator*/ fun <T> of(): MutableList<T> = ArrayList()
}

internal object SetCompanionObject {
    public /*operator*/ fun <T> of(vararg elements: T): Set<T> = elements.toSet()
    public /*operator*/ fun <T> of(element: T): Set<T> = java.util.Collections.singleton(element)
    public /*operator*/ fun <T> of(): Set<T> = EmptySet
}

internal object ArrayCompanionObject {
    public /*operator*/ inline fun <reified T> of(vararg elements: T): Array<T> = elements as Array<T>
}

private class ArrayAsCollection<T>(val values: Array<out T>, val isVarargs: Boolean) : Collection<T> {
    override val size: Int get() = values.size
    override fun isEmpty(): Boolean = values.isEmpty()
    override fun contains(element: T): Boolean = values.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { contains(it) }
    override fun iterator(): Iterator<T> = values.iterator()
    // override hidden toArray implementation to prevent copying of values array
    public fun toArray(): Array<out Any?> = values.copyToArrayOfAny(isVarargs)
}
