/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.jetbrains.kotlin.utils.writeSignedLeb128
import org.jetbrains.kotlin.utils.writeUnsignedLeb128
import org.jetbrains.kotlin.utils.writeUnsignedLeb128Fixed
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

open class ByteWriter(private val os: OutputStream) {
    fun writeByte(v: Byte) {
        os.write(v.toInt())
    }

    fun writeBytes(v: ByteArray) {
        os.write(v)
    }

    fun writeUByte(v: UByte) {
        writeByte(v.toByte())
    }

    fun writeUInt16(v: UShort) {
        writeByte(v.toByte())
        writeByte((v.toUInt() shr 8).toByte())
    }

    fun writeUInt32(v: UInt) {
        writeByte(v.toByte())
        writeByte((v shr 8).toByte())
        writeByte((v shr 16).toByte())
        writeByte((v shr 24).toByte())
    }

    fun writeUInt64(v: ULong) {
        writeByte(v.toByte())
        writeByte((v shr 8).toByte())
        writeByte((v shr 16).toByte())
        writeByte((v shr 24).toByte())
        writeByte((v shr 32).toByte())
        writeByte((v shr 40).toByte())
        writeByte((v shr 48).toByte())
        writeByte((v shr 56).toByte())
    }

    fun writeUInt64(v: ULong, size: Int) =
        when (size) {
            1 -> writeUByte(v.toUByte())
            2 -> writeUInt16(v.toUShort())
            4 -> writeUInt32(v.toUInt())
            8 -> writeUInt64(v)
            else -> error("Unsupported size $size")
        }

    fun writeVarInt7(v: Byte) {
        writeSignedLeb128(v.toLong(), ::writeByte)
    }

    fun writeVarInt32(v: Int) {
        writeSignedLeb128(v.toLong(), ::writeByte)
    }

    fun writeVarInt64(v: Long) {
        writeSignedLeb128(v, ::writeByte)
    }

    fun writeVarUInt1(v: Boolean) {
        writeUnsignedLeb128(if (v) 1u else 0u, ::writeByte)
    }

    fun writeVarUInt7(v: UShort) {
        writeUnsignedLeb128(v.toUInt(), ::writeByte)
    }

    fun writeVarUInt32(v: UInt) {
        writeUnsignedLeb128(v, ::writeByte)
    }

    fun writeVarUInt32FixedSize(v: UInt) {
        writeUnsignedLeb128Fixed(v, ::writeByte)
    }

    fun writeBoolean(value: Boolean) {
        writeByte(if (value) 1 else 0)
    }
}

class WasmBinaryData(private val data: ByteArray, private val size: Int) {
    companion object {
        fun WasmBinaryData.writeTo(file: File) {
            file.outputStream().use {
                it.write(data, 0, size)
            }
        }

        fun WasmBinaryData.toByteArray(): ByteArray = data.copyOf(size)
    }
}

class ByteWriterWithOffsetWrite private constructor(private val os: ByteArrayOutputStreamWithInternals) : ByteWriter(os) {
    constructor() : this(ByteArrayOutputStreamWithInternals())

    private class ByteArrayOutputStreamWithInternals : ByteArrayOutputStream() {
        val streamBuffer: ByteArray get() = this.buf
        var offset
            get() = this.count
            set(value) {
                this.count = value
            }
    }

    val written: Int get() = os.size()

    fun getBinaryData(): WasmBinaryData = WasmBinaryData(os.streamBuffer, written)

    fun writeVarUInt32FixedSize(v: Int, offset: Int) {
        val oldCount = os.offset
        os.offset = offset
        writeVarUInt32FixedSize(v.toUInt())
        os.offset = maxOf(oldCount, os.offset)
    }

    fun writeUInt64(v: ULong, size: Int, offset: Int) {
        val oldCount = os.offset
        os.offset = offset
        writeUInt64(v, size)
        os.offset = maxOf(oldCount, os.offset)
    }
}