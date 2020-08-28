/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import gnu.trove.THashMap

/** Fixed-size ordered collection with no extra space that represents a commonized group of same-rank elements */
class CommonizedGroup<T : Any>(
    override val size: Int,
    initialize: (Int) -> T?
) : AbstractList<T?>() {
    constructor(elements: List<T?>) : this(elements.size, elements::get)

    constructor(size: Int) : this(size, { null })

    // array constructor requires concrete type which is known only at the call site,
    // so let's use `Any?` instead if `T` here
    private val elements = Array<Any?>(size, initialize)

    override operator fun get(index: Int): T? {
        @Suppress("UNCHECKED_CAST")
        return elements[index] as T?
    }

    operator fun set(index: Int, value: T) {
        val oldValue = this[index]
        check(oldValue == null) { "$oldValue can not be overwritten with $value at index $index" }

        elements[index] = value
    }
}

internal class CommonizedGroupMap<K, V : Any>(val size: Int) : Iterable<Map.Entry<K, CommonizedGroup<V>>> {
    private val wrapped: MutableMap<K, CommonizedGroup<V>> = THashMap()

    operator fun get(key: K): CommonizedGroup<V> = wrapped.getOrPut(key) { CommonizedGroup(size) }

    fun getOrNull(key: K): CommonizedGroup<V>? = wrapped[key]

    override fun iterator(): Iterator<Map.Entry<K, CommonizedGroup<V>>> = wrapped.iterator()
}
