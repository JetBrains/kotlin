/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.experimental

/** Performs a bitwise AND operation between the two values. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline infix fun Byte.and(other: Byte): Byte = (this.toInt() and other.toInt()).toByte()

/** Performs a bitwise OR operation between the two values. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline infix fun Byte.or(other: Byte): Byte = (this.toInt() or other.toInt()).toByte()

/** Performs a bitwise XOR operation between the two values. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline infix fun Byte.xor(other: Byte): Byte = (this.toInt() xor other.toInt()).toByte()

/** Inverts the bits in this value. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun Byte.inv(): Byte = (this.toInt().inv()).toByte()


/** Performs a bitwise AND operation between the two values. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline infix fun Short.and(other: Short): Short = (this.toInt() and other.toInt()).toShort()

/** Performs a bitwise OR operation between the two values. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline infix fun Short.or(other: Short): Short = (this.toInt() or other.toInt()).toShort()

/** Performs a bitwise XOR operation between the two values. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline infix fun Short.xor(other: Short): Short = (this.toInt() xor other.toInt()).toShort()

/** Inverts the bits in this value. */
@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
public inline fun Short.inv(): Short = (this.toInt().inv()).toShort()


