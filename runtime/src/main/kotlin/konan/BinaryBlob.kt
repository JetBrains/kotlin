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
package konan

import konan.internal.*
import kotlinx.cinterop.*

/**
 * An immutable compile-time array of bytes.
 */
@ExportTypeInfo("theImmutableBinaryBlobTypeInfo")
@Frozen
public final class ImmutableBinaryBlob private constructor() {
    public val size: Int
        get() = getArrayLength()

    // Data layout is the same as for ByteArray, so we can share native functions.
    @SymbolName("Kotlin_ByteArray_get")
    external public operator fun get(index: Int): Byte

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator {
        return ImmutableBinaryBlobIteratorImpl(this)
    }
}

private class ImmutableBinaryBlobIteratorImpl(
        val collection: ImmutableBinaryBlob) : ByteIterator() {
    var index : Int = 0

    public override fun nextByte(): Byte {
        if (!hasNext()) throw NoSuchElementException("$index")
        return collection[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < collection.size
    }
}

// Allocates new ByteArray and copies the data.
@SymbolName("Kotlin_ImmutableBinaryBlob_toByteArray")
public external fun ImmutableBinaryBlob.toByteArray(start: Int, count: Int): ByteArray
public fun ImmutableBinaryBlob.toByteArray() = toByteArray(0, size)

// Returns stable C pointer to data at certain offset, useful as a way to pass resource
// to C API.
public external fun ImmutableBinaryBlob.asCPointer(offset: Int) =
        interpretCPointer<ByteVar>(asCPointerImpl(offset))
@SymbolName("Kotlin_ImmutableBinaryBlob_asCPointerImpl")
private external fun ImmutableBinaryBlob.asCPointerImpl(offset: Int): konan.internal.NativePtr

// Creates ImmutableBinaryBlob out of compile-time constant data.
// This method accepts Short type, so that values in range 0x80 .. 0xff can be
// provided without toByte() cast. One element still represent one byte in the output data.
// This is the only way to create ImmutableBinaryBlob for now.
// TODO: reconsider?
@Intrinsic
public external fun immutableBinaryBlobOf(vararg elements: Short): ImmutableBinaryBlob
