/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * The common base class of all enum classes.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/enum-classes.html) for more
 * information on enum classes.
 */
public actual abstract class Enum<E : Enum<E>>
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual constructor(
    /**
     * Returns the name of this enum constant, exactly as declared in its enum declaration.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public actual val name: String,
    /**
     * Returns the ordinal of this enumeration constant (its position in its enum declaration, where the initial constant
     * is assigned an ordinal of zero).
     */
    public actual val ordinal: Int
) : Comparable<E> {

    public actual final override fun compareTo(other: E): Int =
        ordinal.compareTo(other.ordinal)

    public actual final override fun equals(other: Any?): Boolean =
        this === other

    public actual final override fun hashCode(): Int =
        super.hashCode()

    public actual override fun toString(): String =
        name

    public actual companion object
}
