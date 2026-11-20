/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE") // for building kotlin-stdlib-jvm-minimal-for-test

package kotlin.enums

import kotlin.internal.ReadObjectParameterType
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.internal.throwReadObjectNotSupported

/**
 * A specialized immutable implementation of [List] interface that
 * contains all enum entries of the specified enum type [E].
 * [EnumEntries] contains all enum entries in the order they are declared in the source code,
 * consistently with the corresponding [Enum.ordinal] values.
 *
 * An instance of this interface can be obtained from `EnumClass.entries` property or with [enumEntries] function.
 *
 * #### Implementation note
 * All basic operations, such as `get`, `contains` and `indexOf`, are executed in constant time and are likely to be
 * faster than regular `ArrayList<E>` counterparts.
 */
@SinceKotlin("1.9")
@WasExperimental(ExperimentalStdlibApi::class)
public sealed interface EnumEntries<E : Enum<E>> : List<E>

/**
 * Returns the enum entry of the enum type [E] with the specified [name].
 *
 * The [name] must exactly match an existing enum constant of type [E] (case-sensitive).
 *
 * ```kotlin
 * enum class Direction { NORTH, SOUTH, EAST, WEST }
 *
 * val east: Direction = Direction.entries.valueOf("EAST")
 * println(east.ordinal) // 2
 *
 * // The lookup fails if a string does not correspond to any of the enum entries
 * Direction.entries.valueOf("UP") // fails with NoSuchElementException
 * Direction.entries.valueOf("") // fails with NoSuchElementException
 *
 * // The lookup is case-sensitive and fails if the name doesn't match any constant
 * Direction.entries.valueOf("east") // fails with NoSuchElementException
 * ```
 *
 * @throws NoSuchElementException if no enum constant with the specified [name] exists in [E].
 */
// @sample samples.misc.Enums.enumEntriesValueOfSample - not runnable due to enum declarations
@SinceKotlin("2.5")
@ExperimentalStdlibApi
public fun <E : Enum<E>> EnumEntries<E>.valueOf(name: String): E =
    first { it.name == name }

/**
 * Returns the enum entry of the enum type [E] with the specified [name] or null if no such entry exists.
 *
 * The [name] must exactly match an existing enum constant of type [E] (case-sensitive).
 *
 * ```kotlin
 * enum class Direction { NORTH, SOUTH, EAST, WEST }
 *
 * val east: Direction? = Direction.entries.valueOfOrNull("EAST")
 * println(east!!.ordinal) // 2
 *
 * // The lookup returns null if a string does not correspond to any of the enum entries
 * println(Direction.entries.valueOfOrNull("UP")) // null
 * println(Direction.entries.valueOfOrNull("")) // null
 *
 * // The lookup is case-sensitive and returns null if the name doesn't match any entry name exactly
 * println(Direction.entries.valueOfOrNull("east")) // null
 * ```
 */
// @sample samples.misc.Enums.enumEntriesValueOfOrNullSample - not runnable due to enum declarations
@SinceKotlin("2.5")
@ExperimentalStdlibApi
public fun <E : Enum<E>> EnumEntries<E>.valueOfOrNull(name: String): E? =
    firstOrNull { it.name == name }

/**
 * Returns [EnumEntries] list containing all enum entries for the given enum type [T].
 *
 * The returned list is immutable, so this function can avoid instantiating a new instance of entries list every time
 * and may return the same cached instance on subsequent invocations.
 *
 * ```kotlin
 * enum class Direction { NORTH, SOUTH, EAST, WEST }
 *
 * val entries = enumEntries<Direction>()
 * println(entries) // prints [NORTH, SOUTH, EAST, WEST]
 * pritnln(entries.indexOf(Direction.EAST)) // 2
 * println(entries[1]) // SOUTH
 *
 * enum class Empty {}
 * println(enumEntries<Empty>()) // prints an empty list, []
 * ```
 *
 * @see EnumEntries
 */
// @sample samples.misc.Enums.enumEntriesSample - not runnable due to enum declarations
@WasExperimental(ExperimentalStdlibApi::class)
@SinceKotlin("2.0")
public inline fun <reified T : Enum<T>> enumEntries(): EnumEntries<T> = enumEntriesIntrinsic()

@PublishedApi
@SinceKotlin("1.9")
internal expect fun <T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T>

@PublishedApi
@SinceKotlin("1.8") // Used by pre-1.9.0 JVM compiler for the feature in preview mode. Can be safely removed around 2.1
internal fun <E : Enum<E>> enumEntries(entriesProvider: () -> Array<E>): EnumEntries<E> = EnumEntriesList(entriesProvider())

@PublishedApi
@SinceKotlin("1.8")
@UsedFromCompilerGeneratedCode
internal fun <E : Enum<E>> enumEntries(entries: Array<E>): EnumEntries<E> = EnumEntriesList(entries)

@SinceKotlin("1.8")
private class EnumEntriesList<T : Enum<T>>(private val entries: Array<T>) : EnumEntries<T>, AbstractList<T>(), RandomAccess, Serializable {
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

    @Suppress("unused")
    private fun readObject(@Suppress("UNUSED_PARAMETER") input: ReadObjectParameterType): Unit = throwReadObjectNotSupported()
}

internal expect class EnumEntriesSerializationProxy<E : Enum<E>>(entries: Array<E>)
