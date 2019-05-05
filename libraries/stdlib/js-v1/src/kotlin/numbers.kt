/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin


/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout.
 */
@SinceKotlin("1.2")
@library("doubleToBits")
public actual fun Double.toBits(): Long = definedExternally

/**
 * Returns a bit representation of the specified floating-point value as [Long]
 * according to the IEEE 754 floating-point "double format" bit layout,
 * preserving `NaN` values exact layout.
 */
@SinceKotlin("1.2")
@library("doubleToRawBits")
public actual fun Double.toRawBits(): Long = definedExternally

/**
 * Returns the [Double] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Double.Companion.fromBits(bits: Long): Double = js("Kotlin").doubleFromBits(bits).unsafeCast<Double>()

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
@library("floatToBits")
public actual fun Float.toBits(): Int = definedExternally

/**
 * Returns a bit representation of the specified floating-point value as [Int]
 * according to the IEEE 754 floating-point "single format" bit layout,
 * preserving `NaN` values exact layout.
 *
 * Note that in Kotlin/JS [Float] range is wider than "single format" bit layout can represent,
 * so some [Float] values may overflow, underflow or loose their accuracy after conversion to bits and back.
 */
@SinceKotlin("1.2")
@library("floatToRawBits")
public actual fun Float.toRawBits(): Int = definedExternally

/**
 * Returns the [Float] value corresponding to a given bit representation.
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public actual inline fun Float.Companion.fromBits(bits: Int): Float = js("Kotlin").floatFromBits(bits).unsafeCast<Float>()