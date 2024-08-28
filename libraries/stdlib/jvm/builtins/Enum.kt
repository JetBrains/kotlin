/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.internal.ProducesBuiltinMetadata

package kotlin

/**
 * The common base class of all enum classes.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/enum-classes.html) for more
 * information on enum classes.
 */
public actual abstract class Enum<E : Enum<E>> actual constructor(name: String, ordinal: Int): Comparable<E> {
    public actual companion object {}

    /**
     * Returns the name of this enum constant, exactly as declared in its enum declaration.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public actual final val name: String

    /**
     * Returns the ordinal of this enumeration constant (its position in its enum declaration, where the initial constant
     * is assigned an ordinal of zero).
     */
    @Suppress("MUST_BE_INITIALIZED_OR_BE_ABSTRACT")
    public actual final val ordinal: Int

    @Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
    public actual override final fun compareTo(other: E): Int

    /**
     * Throws an exception since enum constants cannot be cloned.
     * This method prevents enum classes from inheriting from `Cloneable`.
     */
    @Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
    protected final fun clone(): Any

    @Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
    public actual override final fun equals(other: Any?): Boolean

    @Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
    public actual override final fun hashCode(): Int

    @Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
    public actual override fun toString(): String

    /**
     * Returns an array containing the constants of this enum type, in the order they're declared.
     * This method may be used to iterate over the constants.
     * @values
     */

    /**
     * Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)
     * @throws IllegalArgumentException if this enum type has no constant with the specified name
     * @valueOf
     */
}
