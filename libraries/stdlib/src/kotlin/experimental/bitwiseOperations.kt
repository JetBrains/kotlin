/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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


