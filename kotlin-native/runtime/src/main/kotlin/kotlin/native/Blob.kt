/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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
    @GCCritical
    public external operator fun get(index: Int): Byte

    @SymbolName("Kotlin_ByteArray_getArrayLength")
    @GCCritical
    private external fun getArrayLength(): Int

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
 * Copies the data from this blob into a new [ByteArray].
 *
 * @param startIndex the beginning (inclusive) of the subrange to copy, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to copy, size of this blob by default.
 */
@SymbolName("Kotlin_ImmutableBlob_toByteArray")
@GCCritical
public external fun ImmutableBlob.toByteArray(startIndex: Int = 0, endIndex: Int = size): ByteArray

/**
 * Copies the data from this blob into a new [UByteArray].
 *
 * @param startIndex the beginning (inclusive) of the subrange to copy, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to copy, size of this blob by default.
 */
@ExperimentalUnsignedTypes
@SymbolName("Kotlin_ImmutableBlob_toByteArray")
@GCCritical
public external fun ImmutableBlob.toUByteArray(startIndex: Int = 0, endIndex: Int = size): UByteArray

/**
 * Returns stable C pointer to data at certain [offset], useful as a way to pass resource
 * to C APIs.
 * @see kotlinx.cinterop.CPointer
 */
public fun ImmutableBlob.asCPointer(offset: Int = 0): CPointer<ByteVar> =
        interpretCPointer<ByteVar>(asCPointerImpl(offset))!!

public fun ImmutableBlob.asUCPointer(offset: Int = 0): CPointer<UByteVar> =
        interpretCPointer<UByteVar>(asCPointerImpl(offset))!!

@SymbolName("Kotlin_ImmutableBlob_asCPointerImpl")
@GCCritical
private external fun ImmutableBlob.asCPointerImpl(offset: Int): kotlin.native.internal.NativePtr

/**
 * Creates [ImmutableBlob] out of compile-time constant data.
 *
 * This method accepts values of [Short] type in range `0x00..0xff`, other values are prohibited.
 *
 * One element still represent one byte in the output data.
 * This is the only way to create ImmutableBlob for now.
 */
@TypedIntrinsic(IntrinsicType.IMMUTABLE_BLOB)
public external fun immutableBlobOf(vararg elements: Short): ImmutableBlob
