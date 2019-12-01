/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")

package kotlin

/**
 * Represents a value which is either `true` or `false`. On the JVM, non-nullable values of this type are
 * represented as values of the primitive type `boolean`.
 */
public class Boolean private constructor() : Comparable<Boolean> {
    /**
     * Returns the inverse of this boolean.
     */
    public operator fun not(): Boolean

    /**
     * Performs a logical `and` operation between this Boolean and the [other] one. Unlike the `&&` operator,
     * this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
     */
    public infix fun and(other: Boolean): Boolean

    /**
     * Performs a logical `or` operation between this Boolean and the [other] one. Unlike the `||` operator,
     * this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
     */
    public infix fun or(other: Boolean): Boolean

    /**
     * Performs a logical `xor` operation between this Boolean and the [other] one.
     */
    public infix fun xor(other: Boolean): Boolean

    public override fun compareTo(other: Boolean): Int


    public override fun equals(other: Any?): Boolean

    public override fun hashCode(): Int

    public override fun toString(): String

    @SinceKotlin("1.3")
    companion object {}
}
