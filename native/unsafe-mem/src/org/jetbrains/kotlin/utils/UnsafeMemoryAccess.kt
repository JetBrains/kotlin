/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

/**
 * Many usages of this interface are on the hot path. So, interface call dispatch might have a
 * performance impact.
 * The trick is that only one implementation is class-loaded. So, the calls are monomorphic,
 * and the JVM JIT (even C1) can skip the interface call dispatch and even inline the methods.
 *
 * Currently, there is only one implementation - [UnsafeBasedMemoryAccess]. It uses `sun.misc.Unsafe`.
 * The intention is to add one more, using FFM API. See KT-83647 and KT-83648.
 */
interface UnsafeMemoryAccess {
    fun allocateMemory(size: Long): Long
    fun freeMemory(address: Long)

    fun getByte(address: Long): Byte
    fun putByte(address: Long, value: Byte)

    fun getShort(address: Long): Short
    fun putShort(address: Long, value: Short)

    fun getInt(address: Long): Int
    fun putInt(address: Long, value: Int)

    fun getLong(address: Long): Long
    fun putLong(address: Long, value: Long)

    fun getFloat(address: Long): Float
    fun putFloat(address: Long, value: Float)

    fun getDouble(address: Long): Double
    fun putDouble(address: Long, value: Double)

    fun copyMemory(srcAddress: Long, destAddress: Long, length: Long)
    fun setMemory(address: Long, length: Long, value: Byte)

    fun copyFromByteArray(src: ByteArray, destAddress: Long, length: Int)
    fun copyToByteArray(srcAddress: Long, dest: ByteArray, length: Int)

    fun copyFromCharArray(src: CharArray, destAddress: Long, lengthInChars: Int)
    fun copyToCharArray(srcAddress: Long, dest: CharArray, lengthInChars: Int)
}

val unsafeMemoryAccess: UnsafeMemoryAccess = UnsafeBasedMemoryAccess
