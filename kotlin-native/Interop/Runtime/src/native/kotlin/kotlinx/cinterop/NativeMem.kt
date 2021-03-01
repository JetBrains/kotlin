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

package kotlinx.cinterop

import kotlin.native.*
import kotlin.native.internal.Intrinsic
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

@PublishedApi
internal inline val pointerSize: Int
    get() = getPointerSize()

@PublishedApi
@TypedIntrinsic(IntrinsicType.INTEROP_GET_POINTER_SIZE)
internal external fun getPointerSize(): Int

// TODO: do not use singleton because it leads to init-check on any access.
@PublishedApi
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

    // TODO: optimize
    fun getByteArray(source: NativePointed, dest: ByteArray, length: Int) {
        val sourceArray = source.reinterpret<ByteVar>().ptr
        var index = 0
        while (index < length) {
            dest[index] = sourceArray[index]
            ++index
        }
    }

    // TODO: optimize
    fun putByteArray(source: ByteArray, dest: NativePointed, length: Int) {
        val destArray = dest.reinterpret<ByteVar>().ptr
        var index = 0
        while (index < length) {
            destArray[index] = source[index]
            ++index
        }
    }

    // TODO: optimize
    fun getCharArray(source: NativePointed, dest: CharArray, length: Int) {
        val sourceArray = source.reinterpret<ShortVar>().ptr
        var index = 0
        while (index < length) {
            dest[index] = sourceArray[index].toChar()
            ++index
        }
    }

    // TODO: optimize
    fun putCharArray(source: CharArray, dest: NativePointed, length: Int) {
        val destArray = dest.reinterpret<ShortVar>().ptr
        var index = 0
        while (index < length) {
            destArray[index] = source[index].toShort()
            ++index
        }
    }

    // TODO: optimize
    fun zeroMemory(dest: NativePointed, length: Int): Unit {
        val destArray = dest.reinterpret<ByteVar>().ptr
        var index = 0
        while (index < length) {
            destArray[index] = 0
            ++index
        }
    }

    // TODO: optimize
    fun copyMemory(dest: NativePointed, length: Int, src: NativePointed): Unit {
        val destArray = dest.reinterpret<ByteVar>().ptr
        val srcArray = src.reinterpret<ByteVar>().ptr
        var index = 0
        while (index < length) {
            destArray[index] = srcArray[index]
            ++index
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

public fun CPointer<UShortVar>.toKStringFromUtf16(): String {
    val nativeBytes = this

    var length = 0
    while (nativeBytes[length] != 0.toUShort()) {
        ++length
    }
    val chars = kotlin.CharArray(length)
    var index = 0
    while (index < length) {
        chars[index] = nativeBytes[index].toShort().toChar()
        ++index
    }
    return chars.concatToString()
}

public fun CPointer<ShortVar>.toKString(): String = this.toKStringFromUtf16()

public fun CPointer<UShortVar>.toKString(): String = this.toKStringFromUtf16()

@SymbolName("Kotlin_interop_malloc")
private external fun malloc(size: Long, align: Int): NativePtr

@SymbolName("Kotlin_interop_free")
private external fun cfree(ptr: NativePtr)

@TypedIntrinsic(IntrinsicType.INTEROP_READ_BITS)
external fun readBits(ptr: NativePtr, offset: Long, size: Int, signed: Boolean): Long
@TypedIntrinsic(IntrinsicType.INTEROP_WRITE_BITS)
external fun writeBits(ptr: NativePtr, offset: Long, size: Int, value: Long)
