/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

public fun ByteArray(size: Int) = VArray(size) { 0.toByte() }
public fun CharArray(size: Int) = VArray(size) { '\u0000' }
public fun ShortArray(size: Int) = VArray(size) { 0.toShort() }
public fun IntArray(size: Int) = VArray(size) { 0.toInt() }
public fun LongArray(size: Int) = VArray(size) { 0.toLong() }
public fun FloatArray(size: Int) = VArray(size) { 0.toFloat() }
public fun DoubleArray(size: Int) = VArray(size) { 0.toDouble() }
public fun BooleanArray(size: Int) = VArray(size) { false }

