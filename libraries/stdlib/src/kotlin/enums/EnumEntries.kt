/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
@SinceKotlin("1.8")
internal fun <E : Enum<E>> enumEntries(entriesProvider: () -> Array<E>): EnumEntries<E> = EnumEntriesList(entriesProvider)

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
private class EnumEntriesList<E : Enum<E>>(private val entriesProvider: () -> Array<E>) : EnumEntries<E>, AbstractList<E>() {

    /*
     * Open questions to implementation:
     *
     * - Are we allowed to use e.ordinal as an index?
     *   - e.g. indexOf(e) = e.ordinal
     *
     * - Are we allowed to short-circuit methods?
     *     - e.g. `EEL.contains(anyE)` is always true as long as no reflection is involved
     *
     *  - Should it be Java-serializable? (then we definitely can suffer from short-circuiting and should be extra-careful around read-resolve)
     *
     *  - Should it be sealed or just a class with internal constructor? TODO discuss on design to align this policy over all the language
     *    - Probably should to avoid exposing AbstractList superclass directly?
     *
     *  - TODO package-info for kotlinlang
     */

    @Volatile // Volatile is required for safe publication of the array. It doesn't incur any real-world penalties
    private var _entries: Array<E>? = null
    private val entries: Array<E>
        get() {
            var e = _entries
            if (e != null) return e
            e = entriesProvider()
            _entries = e
            return e
        }

    override val size: Int
        get() = entries.size

    override fun get(index: Int): E {
        val entries = entries
        checkElementIndex(index, entries.size)
        return entries[index]
    }
}
