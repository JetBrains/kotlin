/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.enums

@SinceKotlin("1.9")
@WasExperimental(ExperimentalStdlibApi::class)
public sealed interface EnumEntries<E : Enum<E>> : List<E>

/**
 * Returns [EnumEntries] list containing all enum entries for the given enum type [T].
 */
@WasExperimental(ExperimentalStdlibApi::class)
@SinceKotlin("2.0")
public inline fun <reified T : Enum<T>> enumEntries(): EnumEntries<T> = enumEntriesIntrinsic()

@PublishedApi
@SinceKotlin("1.9")
internal expect fun <T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T>


@PublishedApi
@SinceKotlin("1.8")
internal fun <E : Enum<E>> enumEntries(entries: Array<E>): EnumEntries<E> = EnumEntriesList(entries)

@SinceKotlin("1.8")
private class EnumEntriesList<T : Enum<T>>(entries: Array<T>) : EnumEntries<T> {

    private val entries = entries.asList()

    override val size: Int
        get() = entries.size

    override fun containsAll(elements: Collection<T>): Boolean = entries.containsAll(elements)

    override fun get(index: Int): T {
        return entries[index]
    }

    override fun isEmpty(): Boolean = entries.isEmpty()

    override fun iterator(): Iterator<T> = entries.iterator()

    override fun listIterator(): ListIterator<T> = entries.listIterator()

    override fun listIterator(index: Int): ListIterator<T> = entries.listIterator(0)

    override fun subList(fromIndex: Int, toIndex: Int): List<T> = entries.subList(fromIndex, toIndex)

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

    override fun equals(other: Any?): Boolean = entries == other

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = entries.toString()
}
