/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package support.raw

@Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "ABSTRACT_MEMBER_NOT_IMPLEMENTED",
    "ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED",
)
expect class FloatOrDouble : Number, Comparable<FloatOrDouble> {
    /** Include contents of [support.raw.bases.SignedNumber] */

    operator fun plus(other: FloatOrDouble): FloatOrDouble
    operator fun plus(other: Byte): FloatOrDouble
    operator fun plus(other: Short): FloatOrDouble
    operator fun plus(other: Int): FloatOrDouble
    operator fun plus(other: Long): FloatOrDouble
    operator fun plus(other: Float): FloatOrDouble
    operator fun plus(other: Double): Double

    operator fun minus(other: FloatOrDouble): FloatOrDouble
    operator fun minus(other: Byte): FloatOrDouble
    operator fun minus(other: Short): FloatOrDouble
    operator fun minus(other: Int): FloatOrDouble
    operator fun minus(other: Long): FloatOrDouble
    operator fun minus(other: Float): FloatOrDouble
    operator fun minus(other: Double): Double

    operator fun times(other: FloatOrDouble): FloatOrDouble
    operator fun times(other: Byte): FloatOrDouble
    operator fun times(other: Short): FloatOrDouble
    operator fun times(other: Int): FloatOrDouble
    operator fun times(other: Long): FloatOrDouble
    operator fun times(other: Float): FloatOrDouble
    operator fun times(other: Double): Double

    operator fun div(other: FloatOrDouble): FloatOrDouble
    operator fun div(other: Byte): FloatOrDouble
    operator fun div(other: Short): FloatOrDouble
    operator fun div(other: Int): FloatOrDouble
    operator fun div(other: Long): FloatOrDouble
    operator fun div(other: Float): FloatOrDouble
    operator fun div(other: Double): Double

    operator fun rem(other: FloatOrDouble): FloatOrDouble
    operator fun rem(other: Byte): FloatOrDouble
    operator fun rem(other: Short): FloatOrDouble
    operator fun rem(other: Int): FloatOrDouble
    operator fun rem(other: Long): FloatOrDouble
    operator fun rem(other: Float): FloatOrDouble
    operator fun rem(other: Double): Double

    operator fun unaryPlus(): FloatOrDouble
    operator fun unaryMinus(): FloatOrDouble
}
