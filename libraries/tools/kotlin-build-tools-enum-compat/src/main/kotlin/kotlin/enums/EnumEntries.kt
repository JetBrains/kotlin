/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

import java.io.Serializable

/**
 * A specialized immutable implementation of [List] interface that
 * contains all enum entries of the specified enum type [E].
 * [EnumEntries] contains all enum entries in the order they are declared in the source code,
 * consistently with the corresponding [Enum.ordinal] values.
 *
 * An instance of this interface can only be obtained from `EnumClass.entries` property.
 */
@ExperimentalStdlibApi
interface EnumEntries<E : Enum<E>> : List<E>

@Suppress("unused")
@PublishedApi
@ExperimentalStdlibApi
internal fun <E : Enum<E>> enumEntries(entriesProvider: () -> Array<E>): EnumEntries<E> = EnumEntriesList(entriesProvider())

@PublishedApi
@ExperimentalStdlibApi
internal fun <E : Enum<E>> enumEntries(entries: Array<E>): EnumEntries<E> = EnumEntriesList(entries)

@ExperimentalStdlibApi
private class EnumEntriesList<T : Enum<T>>(private val entries: Array<T>) : EnumEntries<T>, AbstractList<T>(), Serializable {
// WA for JS IR bug:
//  class type parameter name MUST be different from E (AbstractList<E> type parameter),
//  otherwise the bridge names for contains() and indexOf() will be clashed with the original method names,
//  and produced JS code will not contain type checks and will not work correctly.

    override val size: Int
        get() = entries.size

    override fun get(index: Int): T {
        checkElementIndex(index, entries.size)
        return entries[index]
    }

    // Ported from kotlin-stdlib-common AbstractList.kt
    private fun checkElementIndex(index: Int, size: Int) {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }

    // By definition, EnumEntries contains **all** enums in declaration order,
    // thus we are able to short-circuit the implementation here

    override fun contains(element: T): Boolean {
        @Suppress("SENSELESS_COMPARISON")
        if (element === null) return false // WA for JS IR bug
        // Check identity due to UnsafeVariance
        val target = entries.getOrNull(element.ordinal)
        return target === element
    }

    override fun indexOf(element: T): Int {
        @Suppress("SENSELESS_COMPARISON")
        if (element === null) return -1 // WA for JS IR bug
        // Check identity due to UnsafeVariance
        val ordinal = element.ordinal
        val target = entries.getOrNull(ordinal)
        return if (target === element) ordinal else -1
    }

    override fun lastIndexOf(element: T): Int = indexOf(element)

    @Suppress("unused")
    private fun writeReplace(): Any {
        // Used for Java serialization: EESP ensures that deserialized object **always** reflects the state of the enum on the target classpath
        return EnumEntriesSerializationProxy(entries)
    }
}
