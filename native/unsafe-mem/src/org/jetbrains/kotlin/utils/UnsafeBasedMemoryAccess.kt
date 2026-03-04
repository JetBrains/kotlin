/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import sun.misc.Unsafe

internal object UnsafeBasedMemoryAccess : UnsafeMemoryAccess {
    private val unsafe: Unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
        isAccessible = true
        return@with this.get(null) as Unsafe
    }

    private val byteArrayBaseOffset = unsafe.arrayBaseOffset(ByteArray::class.java).toLong()
    private val charArrayBaseOffset = unsafe.arrayBaseOffset(CharArray::class.java).toLong()

    override fun allocateMemory(size: Long): Long = unsafe.allocateMemory(size)
    override fun freeMemory(address: Long): Unit = unsafe.freeMemory(address)

    override fun getByte(address: Long): Byte = unsafe.getByte(address)
    override fun putByte(address: Long, value: Byte): Unit = unsafe.putByte(address, value)

    override fun getShort(address: Long): Short = unsafe.getShort(address)
    override fun putShort(address: Long, value: Short): Unit = unsafe.putShort(address, value)

    override fun getInt(address: Long): Int = unsafe.getInt(address)
    override fun putInt(address: Long, value: Int): Unit = unsafe.putInt(address, value)

    override fun getLong(address: Long): Long = unsafe.getLong(address)
    override fun putLong(address: Long, value: Long): Unit = unsafe.putLong(address, value)

    override fun getFloat(address: Long): Float = unsafe.getFloat(address)
    override fun putFloat(address: Long, value: Float): Unit = unsafe.putFloat(address, value)

    override fun getDouble(address: Long): Double = unsafe.getDouble(address)
    override fun putDouble(address: Long, value: Double): Unit = unsafe.putDouble(address, value)

    override fun copyMemory(srcAddress: Long, destAddress: Long, length: Long): Unit =
        unsafe.copyMemory(srcAddress, destAddress, length)

    override fun setMemory(address: Long, length: Long, value: Byte): Unit =
        unsafe.setMemory(address, length, value)

    override fun copyFromByteArray(src: ByteArray, destAddress: Long, length: Int): Unit =
        unsafe.copyMemory(src, byteArrayBaseOffset, null, destAddress, length.toLong())

    override fun copyToByteArray(srcAddress: Long, dest: ByteArray, length: Int): Unit =
        unsafe.copyMemory(null, srcAddress, dest, byteArrayBaseOffset, length.toLong())

    override fun copyFromCharArray(src: CharArray, destAddress: Long, lengthInChars: Int): Unit =
        unsafe.copyMemory(src, charArrayBaseOffset, null, destAddress, lengthInChars.toLong() * Char.SIZE_BYTES)

    override fun copyToCharArray(srcAddress: Long, dest: CharArray, lengthInChars: Int): Unit =
        unsafe.copyMemory(null, srcAddress, dest, charArrayBaseOffset, lengthInChars.toLong() * Char.SIZE_BYTES)
}
