/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.text

/**
 * Returns a string representation of this [Byte] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
//@kotlin.internal.InlineOnly
public /*inline*/ fun UByte.toString(radix: Int): String = this.toInt().toString(radix)

/**
 * Returns a string representation of this [Short] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
//@kotlin.internal.InlineOnly
public /*inline*/ fun UShort.toString(radix: Int): String = this.toInt().toString(radix)


/**
 * Returns a string representation of this [Int] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
//@kotlin.internal.InlineOnly
public /*inline*/ fun UInt.toString(radix: Int): String = this.toLong().toString(radix)

/**
 * Returns a string representation of this [Long] value in the specified [radix].
 *
 * @throws IllegalArgumentException when [radix] is not a valid radix for number to string conversion.
 */
@SinceKotlin("1.3")
@ExperimentalUnsignedTypes
public fun ULong.toString(radix: Int): String = ulongToString(this.toLong(), checkRadix(radix))


