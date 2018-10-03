/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("Boxing")

@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.coroutines.jvm.internal

/*
 * Box primitive to Java wrapper class by allocating the wrapper object.
 *
 * This allows HotSpot JIT to eliminate allocations completely in coroutines code with primitives.
 */

@SinceKotlin("1.3")
@PublishedApi
internal fun boxBoolean(primitive: Boolean): java.lang.Boolean = java.lang.Boolean.valueOf(primitive) as java.lang.Boolean

@SinceKotlin("1.3")
@PublishedApi
internal fun boxByte(primitive: Byte): java.lang.Byte = java.lang.Byte.valueOf(primitive) as java.lang.Byte

@SinceKotlin("1.3")
@PublishedApi
internal fun boxShort(primitive: Short): java.lang.Short = java.lang.Short(primitive)

@SinceKotlin("1.3")
@PublishedApi
internal fun boxInt(primitive: Int): java.lang.Integer = java.lang.Integer(primitive)

@SinceKotlin("1.3")
@PublishedApi
internal fun boxLong(primitive: Long): java.lang.Long = java.lang.Long(primitive)

@SinceKotlin("1.3")
@PublishedApi
internal fun boxFloat(primitive: Float): java.lang.Float = java.lang.Float(primitive)

@SinceKotlin("1.3")
@PublishedApi
internal fun boxDouble(primitive: Double): java.lang.Double = java.lang.Double(primitive)

@SinceKotlin("1.3")
@PublishedApi
internal fun boxChar(primitive: Char): java.lang.Character = java.lang.Character(primitive)
