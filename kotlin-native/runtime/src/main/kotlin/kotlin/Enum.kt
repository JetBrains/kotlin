/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.native.internal.enumValueOfIntrinsic
import kotlin.native.internal.enumValuesIntrinsic

/**
 * The common base class of all enum classes.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/enum-classes.html) for more
 * information on enum classes.
 */
public actual abstract class Enum<E: Enum<E>>
public actual constructor(
        @kotlin.internal.IntrinsicConstEvaluation
        public actual val name: String,
        public actual val ordinal: Int
): Comparable<E> {

    public actual companion object {
    }

    public actual final override fun compareTo(other: E): Int { return ordinal - other.ordinal }

    public actual final override fun equals(other: Any?): Boolean {
        return this === other
    }

    public actual final override fun hashCode(): Int {
        return super.hashCode()
    }

    public actual override fun toString(): String {
        return name
    }
}

/**
 * Returns the enum entry of type [T] with the specified [name].
 *
 * The [name] must exactly match an existing enum constant of type [T] (case-sensitive).
 *
 * @throws IllegalArgumentException if no enum constant with the specified [name] exists in [T].
 * @sample samples.misc.Enums.enumValueOfSample
 */
public actual inline fun <reified T: Enum<T>> enumValueOf(name: String): T = enumValueOfIntrinsic(name)

/**
 * Returns an array containing enum entries of the enum type [T].
 *
 * The function returns a new instance of the array on every call.
 * The array could be mutated, so working with it may also require defensive copying.
 * Consider using [kotlin.enums.enumEntries] as a more efficient alternative
 * returning an immutable list of enum entries.
 *
 * @see kotlin.enums.enumEntries
 */
public actual inline fun <reified T: Enum<T>> enumValues(): Array<T> = enumValuesIntrinsic()
