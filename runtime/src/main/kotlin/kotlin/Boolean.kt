/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin

/**
 * Represents a value which is either `true` or `false`. On the JVM, non-nullable values of this type are
 * represented as values of the primitive type `boolean`.
 */
public class Boolean private constructor(
        private val value: kotlin.native.internal.BooleanValue) : Comparable<Boolean> {

    @SinceKotlin("1.3")
    companion object {}

    /**
     * Returns the inverse of this boolean.
     */
    @SymbolName("Kotlin_Boolean_not")
    external public operator fun not(): Boolean

    /**
     * Performs a logical `and` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_and_Boolean")
    external public infix fun and(other: Boolean): Boolean

    /**
     * Performs a logical `or` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_or_Boolean")
    external public infix fun or(other: Boolean): Boolean

    /**
     * Performs a logical `xor` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_xor_Boolean")
    external public infix fun xor(other: Boolean): Boolean

    @SymbolName("Kotlin_Boolean_compareTo_Boolean")
    external public override fun compareTo(other: Boolean): Int

    public fun equals(other: Boolean): Boolean = kotlin.native.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
        other is Boolean && kotlin.native.internal.areEqualByValue(this, other)

    public override fun toString() = if (this) "true" else "false"

    public override fun hashCode() = if (this) 1 else 0
}