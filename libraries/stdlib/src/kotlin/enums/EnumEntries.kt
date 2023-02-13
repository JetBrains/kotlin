/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE") // for building kotlin-stdlib-jvm-minimal-for-test

package kotlin.enums

import kotlin.jvm.Volatile

/**
 * A specialized immutable implementation of [List] interface that
 * contains all enum entries of the specified enum type [E].
 * [EnumEntries] contains all enum entries in the order they are declared in the source code,
 * consistently with the corresponding [Enum.ordinal] values.
 *
 * An instance of this interface can only be obtained from `EnumClass.entries` property.
 */
@ExperimentalStdlibApi
@SinceKotlin("1.8")
public sealed interface EnumEntries<E : Enum<E>> : List<E>

@PublishedApi
@ExperimentalStdlibApi
@SinceKotlin("1.8") // Used by JVM compiler
internal fun <E : Enum<E>> enumEntries(entriesProvider: () -> Array<E>): EnumEntries<E> = EnumEntriesList(entriesProvider)

@PublishedApi
@ExperimentalStdlibApi
@SinceKotlin("1.8") // Used by Native/JS compilers and Java serialization
internal fun <E : Enum<E>> enumEntries(entries: Array<E>): EnumEntries<E> = EnumEntriesList { entries }.also {
    /*
     * Here we are enforcing initialization of _entries property.
     * It is required because of two reasons.
     *   1. In old Native mm the object will be frozen after creation, so it must be immutable
     *   2. Native doesn't support @Volatile for now, so this initialization is not generally safe, if
     *      done after object is published.
     *
     * This is very implementation-dependent hack, and it should be removed when/if both reasons above are gone.
     */
    it.size
}

/*
 * For enum class E, this class is instantiated in the following manner (NB it's pseudocode that does not
 * reflect code generation strategy precisely):
 * ```
 * class E extends Enum<E> {
 *    private static final E[] $VALUES
 *    private static final EnumEntries[] $ENTRIES
 *
 *    static {
 *        $VALUES = $values();
 *        val supplier = #invokedynamic ..args.. values;
 *        $ENTRIES = new EnumEntriesList(supplier);
 *    }
 *
 *    public static EnumEntries<MyEnum> getEntries() {
 *        return $ENTRIES;
 *    }
 *
 *    private synthetic static E[] $values() {
 *        return new E[] { ... };
 *    }
 * }
 * ```
 *
 * This machinery is required as a workaround for a long-standing issue when people do reflectively change `$VALUES` of
 * enums in order to workaround project-specific issues.
 * We allow racy initialization (e.g. entriesProvider can be invoked multiple times), but the resulting array is safely
 * published, preventing any read races after the initialization.
 */
@SinceKotlin("1.8")
@ExperimentalStdlibApi
private class EnumEntriesList<T : Enum<T>>(private val entriesProvider: () -> Array<T>) : EnumEntries<T>, AbstractList<T>(), Serializable {
// WA for JS IR bug:
//  class type parameter MUST be different form E (AbstractList<E> type parameter),
//  otherwise the bridge names for contains() and indexOf() will be clashed with the original method names,
//  and produced JS code will not contain type checks and will not work correctly.

    @Volatile // Volatile is required for safe publication of the array. It doesn't incur any real-world penalties
    private var _entries: Array<T>? = null
    private val entries: Array<T>
        get() {
            var e = _entries
            if (e != null) return e
            e = entriesProvider()
            _entries = e
            return e
        }

    override val size: Int
        get() = entries.size

    override fun get(index: Int): T {
        val entries = entries
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

    @Suppress("unused") // Used for Java serialization
    private fun writeReplace(): Any {
        return EnumEntriesSerializationProxy(entries)
    }
}

internal expect class EnumEntriesSerializationProxy<E : Enum<E>>(entries: Array<E>)
