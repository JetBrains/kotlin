/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

/** Represents a value which is either `true` or `false`. */
public class Boolean private constructor() : Comparable<Boolean> {
    @SinceKotlin("1.3")
    companion object {}

    /** Returns the inverse of this boolean. */
    @kotlin.internal.IntrinsicConstEvaluation
    @TypedIntrinsic(IntrinsicType.NOT)
    external public operator fun not(): Boolean

    /**
     * Performs a logical `and` operation between this Boolean and the [other] one. Unlike the `&&` operator,
     * this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    @TypedIntrinsic(IntrinsicType.AND)
    external public infix fun and(other: Boolean): Boolean

    /**
     * Performs a logical `or` operation between this Boolean and the [other] one. Unlike the `||` operator,
     * this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    @TypedIntrinsic(IntrinsicType.OR)
    external public infix fun or(other: Boolean): Boolean

    /** Performs a logical `xor` operation between this Boolean and the [other] one. */
    @kotlin.internal.IntrinsicConstEvaluation
    @TypedIntrinsic(IntrinsicType.XOR)
    external public infix fun xor(other: Boolean): Boolean

    @kotlin.internal.IntrinsicConstEvaluation
    @TypedIntrinsic(IntrinsicType.UNSIGNED_COMPARE_TO)
    external public override fun compareTo(other: Boolean): Int

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun toString(): String = if (this) "true" else "false"

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean =
        other is Boolean && kotlin.native.internal.areEqualByValue(this, other)

    public override fun hashCode(): Int =
        if (this) 1231 else 1237

    @Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
    @kotlin.internal.IntrinsicConstEvaluation
    public fun equals(other: Boolean): Boolean = kotlin.native.internal.areEqualByValue(this, other)
}
