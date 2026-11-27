/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.cinterop

import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.native.*
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.Intrinsic
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

@PublishedApi
internal inline val pointerSize: Int
    get() = getPointerSize()

@PublishedApi
@TypedIntrinsic(IntrinsicType.INTEROP_GET_POINTER_SIZE)
internal external fun getPointerSize(): Int

private const val bulkArrayThresholdBytes = 16
private const val interopChunkBytes = 256 * 1024

// A loop in Kotlin gives the runtime a safepoint between bulk operations.
private inline fun forEachInteropChunk(byteCount: Long, action: (bytesDone: Long, chunkBytes: Int) -> Unit) {
    var bytesDone = 0L
    while (bytesDone < byteCount) {
        val chunkBytes = minOf(byteCount - bytesDone, interopChunkBytes.toLong()).toInt()
        action(bytesDone, chunkBytes)
        bytesDone += chunkBytes
    }
}

@GCUnsafeCall("Kotlin_interop_getByteArrayChunk")
private external fun getByteArrayChunk(source: NativePtr, dest: ByteArray, byteCount: Long, bytesDone: Long, chunkBytes: Int)

@GCUnsafeCall("Kotlin_interop_putByteArrayChunk")
private external fun putByteArrayChunk(source: ByteArray, dest: NativePtr, byteCount: Long, bytesDone: Long, chunkBytes: Int)

@GCUnsafeCall("Kotlin_interop_getCharArrayChunk")
private external fun getCharArrayChunk(source: NativePtr, dest: CharArray, byteCount: Long, bytesDone: Long, chunkBytes: Int)

@GCUnsafeCall("Kotlin_interop_putCharArrayChunk")
private external fun putCharArrayChunk(source: CharArray, dest: NativePtr, byteCount: Long, bytesDone: Long, chunkBytes: Int)

@GCUnsafeCall("Kotlin_interop_zeroMemoryChunk")
private external fun zeroMemoryChunk(dest: NativePtr, chunkBytes: Int)

@GCUnsafeCall("Kotlin_interop_copyMemoryChunk")
private external fun copyMemoryChunk(dest: NativePtr, src: NativePtr, byteCount: Long, bytesDone: Long, chunkBytes: Int)

// TODO: do not use singleton because it leads to init-check on any access.
@PublishedApi
@UsedFromCompilerGeneratedCode
internal object nativeMemUtils {
    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getByte(mem: NativePointed): Byte
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putByte(mem: NativePointed, value: Byte)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getShort(mem: NativePointed): Short
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putShort(mem: NativePointed, value: Short)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getInt(mem: NativePointed): Int
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putInt(mem: NativePointed, value: Int)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getLong(mem: NativePointed): Long
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putLong(mem: NativePointed, value: Long)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getFloat(mem: NativePointed): Float
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putFloat(mem: NativePointed, value: Float)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getDouble(mem: NativePointed): Double
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putDouble(mem: NativePointed, value: Double)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getNativePtr(mem: NativePointed): NativePtr
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putNativePtr(mem: NativePointed, value: NativePtr)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE) external fun getVector(mem: NativePointed): Vector128
    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE) external fun putVector(mem: NativePointed, value: Vector128)

    fun getByteArray(source: NativePointed, dest: ByteArray, length: Int) {
        if (length < bulkArrayThresholdBytes || length > dest.size) {
            val sourceArray = source.reinterpret<ByteVar>().ptr
            var index = 0
            while (index < length) {
                dest[index] = sourceArray[index]
                ++index
            }
            return
        }

        val src = source.rawPtr
        val byteCount = length.toLong()
        forEachInteropChunk(byteCount) { bytesDone, chunkBytes ->
            getByteArrayChunk(src, dest, byteCount, bytesDone, chunkBytes)
        }
    }

    fun putByteArray(source: ByteArray, dest: NativePointed, length: Int) {
        if (length < bulkArrayThresholdBytes || length > source.size) {
            val destArray = dest.reinterpret<ByteVar>().ptr
            var index = 0
            while (index < length) {
                destArray[index] = source[index]
                ++index
            }
            return
        }

        val dst = dest.rawPtr
        val byteCount = length.toLong()
        forEachInteropChunk(byteCount) { bytesDone, chunkBytes ->
            putByteArrayChunk(source, dst, byteCount, bytesDone, chunkBytes)
        }
    }

    fun getCharArray(source: NativePointed, dest: CharArray, length: Int) {
        if (length < bulkArrayThresholdBytes / Char.SIZE_BYTES || length > dest.size) {
            val sourceArray = source.reinterpret<ShortVar>().ptr
            var index = 0
            while (index < length) {
                dest[index] = sourceArray[index].toInt().toChar()
                ++index
            }
            return
        }

        val src = source.rawPtr
        val byteCount = length.toLong() * Char.SIZE_BYTES
        forEachInteropChunk(byteCount) { bytesDone, chunkBytes ->
            getCharArrayChunk(src, dest, byteCount, bytesDone, chunkBytes)
        }
    }

    fun putCharArray(source: CharArray, dest: NativePointed, length: Int) {
        if (length < bulkArrayThresholdBytes / Char.SIZE_BYTES || length > source.size) {
            val destArray = dest.reinterpret<ShortVar>().ptr
            var index = 0
            while (index < length) {
                destArray[index] = source[index].code.toShort()
                ++index
            }
            return
        }

        val dst = dest.rawPtr
        val byteCount = length.toLong() * Char.SIZE_BYTES
        forEachInteropChunk(byteCount) { bytesDone, chunkBytes ->
            putCharArrayChunk(source, dst, byteCount, bytesDone, chunkBytes)
        }
    }

    fun zeroMemory(dest: NativePointed, length: Int) {
        val dst = dest.rawPtr
        forEachInteropChunk(length.toLong()) { bytesDone, chunkBytes ->
            zeroMemoryChunk(dst + bytesDone, chunkBytes)
        }
    }

    fun copyMemory(dest: NativePointed, length: Int, src: NativePointed) {
        val dst = dest.rawPtr
        val source = src.rawPtr
        val byteCount = length.toLong()
        forEachInteropChunk(byteCount) { bytesDone, chunkBytes ->
            copyMemoryChunk(dst, source, byteCount, bytesDone, chunkBytes)
        }
    }

    fun alloc(size: Long, align: Int): NativePointed {
        return interpretOpaquePointed(allocRaw(size, align))
    }

    fun free(mem: NativePtr) {
        freeRaw(mem)
    }

    internal fun allocRaw(size: Long, align: Int): NativePtr {
        val ptr = malloc(size, align)
        if (ptr == nativeNullPtr) {
            throw OutOfMemoryError("unable to allocate native memory")
        }
        return ptr
    }

    internal fun freeRaw(mem: NativePtr) {
        cfree(mem)
    }
}

@ExperimentalForeignApi
public fun CPointer<UShortVar>.toKStringFromUtf16(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toUShort()) {
        ++length
    }
    val chars = kotlin.CharArray(length)
    var index = 0
    while (index < length) {
        chars[index] = nativeBytes[index].toInt().toChar()
        ++index
    }
    return chars.concatToString()
}

@ExperimentalForeignApi
public fun CPointer<ShortVar>.toKString(): String = this.toKStringFromUtf16()

@ExperimentalForeignApi
public fun CPointer<UShortVar>.toKString(): String = this.toKStringFromUtf16()

@GCUnsafeCall("Kotlin_interop_malloc")
private external fun malloc(size: Long, align: Int): NativePtr

@GCUnsafeCall("Kotlin_interop_free")
private external fun cfree(ptr: NativePtr)

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_READ_BITS)
public external fun readBits(ptr: NativePtr, offset: Long, size: Int, signed: Boolean): Long

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_WRITE_BITS)
public external fun writeBits(ptr: NativePtr, offset: Long, size: Int, value: Long)
