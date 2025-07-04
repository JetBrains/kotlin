/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch", "NOTHING_TO_INLINE")

package kotlin.js.internal

/**
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt
 */
internal external class BigInt {
    companion object {
        /**
         * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/asIntN
         */
        fun asIntN(@Suppress("unused") bits: Int, @Suppress("unused") bigint: BigInt): BigInt

        /**
         * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/asUintN
         */
        fun asUintN(@Suppress("unused") bits: Int, @Suppress("unused") bigint: BigInt): BigInt
    }

    /**
     * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/toString
     */
    fun toString(radix: Int = definedExternally): String
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt/BigInt
 */
internal external fun BigInt(value: Number): BigInt

internal fun BigInt.toNumber(): Number {
    val self = this
    return js("Number(self)").unsafeCast<Number>()
}

internal inline operator fun BigInt.unaryMinus(): BigInt = (-asDynamic()).unsafeCast<BigInt>()

internal inline operator fun BigInt.plus(other: BigInt): BigInt = asDynamic() + other.asDynamic()

internal inline operator fun BigInt.minus(other: BigInt): BigInt = asDynamic() - other.asDynamic()

internal inline operator fun BigInt.times(other: BigInt): BigInt = asDynamic() * other.asDynamic()

internal inline operator fun BigInt.div(other: BigInt): BigInt = asDynamic() / other.asDynamic()

internal inline operator fun BigInt.rem(other: BigInt): BigInt = asDynamic() % other.asDynamic()

internal inline infix fun BigInt.and(other: BigInt): BigInt {
    val self = this
    return js("self & other").unsafeCast<BigInt>()
}

internal inline infix fun BigInt.shl(other: BigInt): BigInt {
    val self = this
    return js("self << other").unsafeCast<BigInt>()
}

internal inline infix fun BigInt.shr(other: BigInt): BigInt {
    val self = this
    return js("self >> other").unsafeCast<BigInt>()
}

internal inline val BigInt.isNegative: Boolean
    get() = asDynamic() < 0

internal inline val BigInt.isZero: Boolean
    get() = asDynamic() == 0

internal fun BigInt.abs(): BigInt = if (isNegative) -this else this
