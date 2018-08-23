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
package kotlin.native

import kotlin.native.internal.*
import kotlinx.cinterop.*

/**
 * An immutable compile-time array of bytes.
 */
@ExportTypeInfo("theImmutableBlobTypeInfo")
public final class ImmutableBlob private constructor() {
    public val size: Int
        get() = getArrayLength()

    // Data layout is the same as for ByteArray, so we can share native functions.
    @SymbolName("Kotlin_ByteArray_get")
    external public operator fun get(index: Int): Byte

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    external private fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator {
        return ImmutableBlobIteratorImpl(this)
    }
}

private class ImmutableBlobIteratorImpl(val blob: ImmutableBlob) : ByteIterator() {
    var index : Int = 0

    public override fun nextByte(): Byte {
        if (!hasNext()) throw NoSuchElementException("$index")
        return blob[index++]
    }

    public override operator fun hasNext(): Boolean {
        return index < blob.size
    }
}

/**
 * Allocates new ByteArray or UByteArray and copies the data from blob.
 */
@SymbolName("Kotlin_ImmutableBlob_toByteArray")
public external fun ImmutableBlob.toByteArray(start: Int = 0, count: Int = size): ByteArray

@ExperimentalUnsignedTypes
@SymbolName("Kotlin_ImmutableBlob_toByteArray")
public external fun ImmutableBlob.toUByteArray(start: Int = 0, count: Int = size): UByteArray

/**
 * Returns stable C pointer to data at certain offset, useful as a way to pass resource
 * to C APIs.
 */
public fun ImmutableBlob.asCPointer(offset: Int = 0): CPointer<ByteVar> =
        interpretCPointer<ByteVar>(asCPointerImpl(offset))!!

/*
public fun ImmutableBlob.asUCPointer(offset: Int = 0): CPointer<UByteVar> =
        interpretCPointer<UByteVar>(asCPointerImpl(offset))!!
*/

@SymbolName("Kotlin_ImmutableBlob_asCPointerImpl")
private external fun ImmutableBlob.asCPointerImpl(offset: Int): kotlin.native.internal.NativePtr

/**
 * Creates ImmutableBlob out of compile-time constant data.
 * This method accepts Short type, so that values in range 0x80 .. 0xff can be
 * provided without toByte() cast or 'u' suffix.
 * One element still represent one byte in the output data.
 * This is the only way to create ImmutableBlob for now.
 */
@Intrinsic
public external fun immutableBlobOf(vararg elements: Short): ImmutableBlob
