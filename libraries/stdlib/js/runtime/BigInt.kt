/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package kotlin.js.internal

import kotlin.js.internal.longAsBigInt.BigIntLongImplementation

/**
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/BigInt
 */
@BigIntLongImplementation
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
@BigIntLongImplementation
internal external fun BigInt(value: Any): BigInt

@Suppress("NOTHING_TO_INLINE")
@BigIntLongImplementation
internal inline operator fun BigInt.plus(other: BigInt): BigInt = asDynamic() + other.asDynamic()

@Suppress("NOTHING_TO_INLINE")
@BigIntLongImplementation
internal inline operator fun BigInt.minus(other: BigInt): BigInt = asDynamic() - other.asDynamic()

@Suppress("NOTHING_TO_INLINE")
@BigIntLongImplementation
internal inline operator fun BigInt.times(other: BigInt): BigInt = asDynamic() * other.asDynamic()

@Suppress("NOTHING_TO_INLINE")
@BigIntLongImplementation
internal inline operator fun BigInt.div(other: BigInt): BigInt = asDynamic() / other.asDynamic()

@Suppress("NOTHING_TO_INLINE")
@BigIntLongImplementation
internal inline operator fun BigInt.rem(other: BigInt): BigInt = asDynamic() % other.asDynamic()

@Suppress("NOTHING_TO_INLINE")
@BigIntLongImplementation
internal inline infix fun BigInt.shl(other: BigInt): BigInt {
    @Suppress("UnusedVariable") val self = this
    return js("self << other").unsafeCast<BigInt>()
}

@Suppress("NOTHING_TO_INLINE")
@BigIntLongImplementation
internal inline infix fun BigInt.shr(other: BigInt): BigInt {
    @Suppress("UnusedVariable") val self = this
    return js("self >> other").unsafeCast<BigInt>()
}
