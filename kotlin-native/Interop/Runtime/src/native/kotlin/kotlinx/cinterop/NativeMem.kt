/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.cinterop

import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.IntrinsicType
import kotlin.native.internal.TypedIntrinsic

@PublishedApi
internal inline val pointerSize: Int
    get() = getPointerSize()

@PublishedApi
@TypedIntrinsic(IntrinsicType.INTEROP_GET_POINTER_SIZE)
internal external fun getPointerSize(): Int

// TODO: do not use singleton because it leads to init-check on any access.
@PublishedApi
internal object nativeMemUtils {
    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getByte(mem: NativePointed): Byte

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putByte(mem: NativePointed, value: Byte)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getShort(mem: NativePointed): Short

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putShort(mem: NativePointed, value: Short)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getInt(mem: NativePointed): Int

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putInt(mem: NativePointed, value: Int)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getLong(mem: NativePointed): Long

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putLong(mem: NativePointed, value: Long)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getFloat(mem: NativePointed): Float

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putFloat(mem: NativePointed, value: Float)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getDouble(mem: NativePointed): Double

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putDouble(mem: NativePointed, value: Double)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getNativePtr(mem: NativePointed): NativePtr

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putNativePtr(mem: NativePointed, value: NativePtr)

    @TypedIntrinsic(IntrinsicType.INTEROP_READ_PRIMITIVE)
    external fun getVector(mem: NativePointed): Vector128

    @TypedIntrinsic(IntrinsicType.INTEROP_WRITE_PRIMITIVE)
    external fun putVector(mem: NativePointed, value: Vector128)

    @TypedIntrinsic(IntrinsicType.INTEROP_SET_MEMORY)
    external fun memset(mem: NativePointed, value: Byte, size: Int)

    @TypedIntrinsic(IntrinsicType.INTEROP_SET_MEMORY)
    external fun memset(mem: NativePointed, value: Byte, size: Long)

    @TypedIntrinsic(IntrinsicType.INTEROP_COPY_MEMORY)
    external fun memcpy(dstMem: NativePointed, srcMem: NativePointed, size: Int)

    @TypedIntrinsic(IntrinsicType.INTEROP_COPY_MEMORY)
    external fun memcpy(dstMem: NativePointed, srcMem: NativePointed, size: Long)

    @TypedIntrinsic(IntrinsicType.INTEROP_MOVE_MEMORY)
    external fun memmove(dstMem: NativePointed, srcMem: NativePointed, size: Int)

    @TypedIntrinsic(IntrinsicType.INTEROP_MOVE_MEMORY)
    external fun memmove(dstMem: NativePointed, srcMem: NativePointed, size: Long)

    @TypedIntrinsic(IntrinsicType.INTEROP_COMPARE_MEMORY)
    external fun memcmp(memA: NativePointed, memB: NativePointed, size: Long): Int

    fun getByteArray(source: NativePointed, dest: ByteArray, length: Int) {
        dest.usePinned { pinnedDest ->
            memcpy(pinnedDest.addressOf(0).pointed, source, length)
        }
    }

    fun putByteArray(source: ByteArray, dest: NativePointed, length: Int) {
        source.usePinned { pinnedSrc ->
            memcpy(dest, pinnedSrc.addressOf(0).pointed, length)
        }
    }

    fun getCharArray(source: NativePointed, dest: CharArray, length: Int) {
        dest.usePinned { pinnedDest ->
            memcpy(pinnedDest.addressOf(0).pointed, source, length * Char.SIZE_BYTES)
        }
    }

    fun putCharArray(source: CharArray, dest: NativePointed, length: Int) {
        source.usePinned { pinnedSrc ->
            memcpy(dest, pinnedSrc.addressOf(0).pointed, length * Char.SIZE_BYTES)
        }
    }

    fun getFloatArray(source: NativePointed, dest: FloatArray, length: Int) {
        dest.usePinned { pinnedDest ->
            memcpy(pinnedDest.addressOf(0).pointed, source, length * Float.SIZE_BYTES)
        }
    }

    fun putFloatArray(source: FloatArray, dest: NativePointed, length: Int) {
        source.usePinned { pinnedSrc ->
            memcpy(dest, pinnedSrc.addressOf(0).pointed, length * Float.SIZE_BYTES)
        }
    }

    fun zeroMemory(dest: NativePointed, length: Int): Unit {
        memset(dest, 0, length)
    }

    fun copyMemory(dest: NativePointed, length: Int, src: NativePointed): Unit {
        memcpy(dest, src, length)
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
    return CharArray(length).apply {
        nativeMemUtils.getCharArray(pointed, this, length)
    }.concatToString()
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
