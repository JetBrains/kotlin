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

import org.jetbrains.kotlin.utils.unsafeMemoryAccess
import org.jetbrains.kotlin.utils.nativeMemoryAllocator

private val NativePointed.address: Long
    get() = this.rawPtr

private enum class DataModel(val pointerSize: Long) {
    _32BIT(4),
    _64BIT(8)
}

private val dataModel: DataModel = when (System.getProperty("sun.arch.data.model")) {
    null -> TODO()
    "32" -> DataModel._32BIT
    "64" -> DataModel._64BIT
    else -> throw IllegalStateException()
}

// Must be only used in interop, contains host pointer size, not target!
@PublishedApi
internal val pointerSize: Int = dataModel.pointerSize.toInt()

@PublishedApi
internal object nativeMemUtils {
    fun getByte(mem: NativePointed) = unsafeMemoryAccess.getByte(mem.address)
    fun putByte(mem: NativePointed, value: Byte) = unsafeMemoryAccess.putByte(mem.address, value)

    fun getShort(mem: NativePointed) = unsafeMemoryAccess.getShort(mem.address)
    fun putShort(mem: NativePointed, value: Short) = unsafeMemoryAccess.putShort(mem.address, value)

    fun getInt(mem: NativePointed) = unsafeMemoryAccess.getInt(mem.address)
    fun putInt(mem: NativePointed, value: Int) = unsafeMemoryAccess.putInt(mem.address, value)

    fun getLong(mem: NativePointed) = unsafeMemoryAccess.getLong(mem.address)
    fun putLong(mem: NativePointed, value: Long) = unsafeMemoryAccess.putLong(mem.address, value)

    fun getFloat(mem: NativePointed) = unsafeMemoryAccess.getFloat(mem.address)
    fun putFloat(mem: NativePointed, value: Float) = unsafeMemoryAccess.putFloat(mem.address, value)

    fun getDouble(mem: NativePointed) = unsafeMemoryAccess.getDouble(mem.address)
    fun putDouble(mem: NativePointed, value: Double) = unsafeMemoryAccess.putDouble(mem.address, value)

    fun getNativePtr(mem: NativePointed): NativePtr = when (dataModel) {
        DataModel._32BIT -> getInt(mem).toLong()
        DataModel._64BIT -> getLong(mem)
    }

    fun putNativePtr(mem: NativePointed, value: NativePtr) = when (dataModel) {
        DataModel._32BIT -> putInt(mem, value.toInt())
        DataModel._64BIT -> putLong(mem, value)
    }

    fun getByteArray(source: NativePointed, dest: ByteArray, length: Int) {
        unsafeMemoryAccess.copyToByteArray(source.address, dest, length)
    }

    fun putByteArray(source: ByteArray, dest: NativePointed, length: Int) {
        unsafeMemoryAccess.copyFromByteArray(source, dest.address, length)
    }

    fun getCharArray(source: NativePointed, dest: CharArray, length: Int) {
        unsafeMemoryAccess.copyToCharArray(source.address, dest, lengthInChars = length)
    }

    fun putCharArray(source: CharArray, dest: NativePointed, length: Int) {
        unsafeMemoryAccess.copyFromCharArray(source, dest.address, lengthInChars = length)
    }

    fun zeroMemory(dest: NativePointed, length: Int): Unit =
            unsafeMemoryAccess.setMemory(dest.address, length.toLong(), 0)

    fun copyMemory(dest: NativePointed, length: Int, src: NativePointed) =
            unsafeMemoryAccess.copyMemory(src.address, dest.address, length.toLong())

    internal fun allocRaw(size: Long, align: Int): NativePtr {
        val address = unsafeMemoryAccess.allocateMemory(size)
        if (address % align != 0L) TODO(align.toString())
        return address
    }

    internal fun freeRaw(mem: NativePtr) {
        unsafeMemoryAccess.freeMemory(mem)
    }

    fun alloc(size: Long, align: Int) = interpretOpaquePointed(nativeMemoryAllocator.alloc(size, align))

    fun free(mem: NativePtr) = nativeMemoryAllocator.free(mem)
}
