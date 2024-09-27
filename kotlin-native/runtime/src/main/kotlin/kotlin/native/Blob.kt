/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native

import kotlin.native.internal.*
import kotlinx.cinterop.*
import kotlin.native.internal.escapeAnalysis.Escapes

/**
 * An immutable compile-time array of bytes.
 */
@Deprecated("Use ByteArray instead.")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public final class ImmutableBlob private constructor() {
    public val size: Int
        get() = getArrayLength()

    // Data layout is the same as for ByteArray, so we can share native functions.
    @GCUnsafeCall("Kotlin_ByteArray_get")
    @Escapes.Nothing
    public external operator fun get(index: Int): Byte

    @GCUnsafeCall("Kotlin_ByteArray_getArrayLength")
    @Escapes.Nothing
    private external fun getArrayLength(): Int

    /** Creates an iterator over the elements of the array. */
    public operator fun iterator(): ByteIterator {
        return ImmutableBlobIteratorImpl(this)
    }
}

@Suppress("DEPRECATION_ERROR")
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
@Suppress("DEPRECATION_ERROR")
@Deprecated("ImmutableBlob is deprecated. Use ByteArray instead.")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
@GCUnsafeCall("Kotlin_ImmutableBlob_toByteArray")
@Escapes.Nothing
public external fun ImmutableBlob.toByteArray(startIndex: Int = 0, endIndex: Int = size): ByteArray

/**
 * Copies the data from this blob into a new [UByteArray].
 *
 * @param startIndex the beginning (inclusive) of the subrange to copy, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to copy, size of this blob by default.
 */
@Suppress("DEPRECATION_ERROR")
@Deprecated("ImmutableBlob is deprecated. Use ByteArray instead.")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
@ExperimentalUnsignedTypes
@GCUnsafeCall("Kotlin_ImmutableBlob_toByteArray")
@Escapes.Nothing
public external fun ImmutableBlob.toUByteArray(startIndex: Int = 0, endIndex: Int = size): UByteArray

/**
 * Returns stable C pointer to data at certain [offset], useful as a way to pass resource
 * to C APIs.
 *
 * `ImmutableBlob` is deprecated since Kotlin 1.9. It is recommended to use `ByteArray` instead.
 * To get a stable C pointer to `ByteArray` data the array needs to be pinned first.
 * ```
 * byteArray.usePinned {
 *     val cpointer = it.addressOf(offset)
 *     // use the stable C pointer
 * }
 * ```
 * @see kotlinx.cinterop.CPointer
 */
@Suppress("DEPRECATION_ERROR")
@Deprecated("ImmutableBlob is deprecated. Use ByteArray instead. To get a stable C pointer to a `ByteArray`, pin it first.")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public fun ImmutableBlob.asCPointer(offset: Int = 0): CPointer<ByteVar> =
        interpretCPointer<ByteVar>(asCPointerImpl(offset))!!

/**
 * Returns stable C pointer to data at certain [offset], useful as a way to pass resource
 * to C APIs.
 *
 * `ImmutableBlob` is deprecated since Kotlin 1.9. It is recommended to use `ByteArray` instead.
 * To get a stable C pointer to `ByteArray` data the array needs to be pinned first.
 * ```
 * byteArray.usePinned {
 *     val cpointer = it.addressOf(offset)
 *     // use the stable C pointer
 * }
 * ```
 * @see kotlinx.cinterop.CPointer
 */
@Suppress("DEPRECATION_ERROR")
@Deprecated("ImmutableBlob is deprecated. Use ByteArray instead. To get a stable C pointer to a `ByteArray`, pin it first.")
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
public fun ImmutableBlob.asUCPointer(offset: Int = 0): CPointer<UByteVar> =
        interpretCPointer<UByteVar>(asCPointerImpl(offset))!!

@Suppress("DEPRECATION_ERROR")
@GCUnsafeCall("Kotlin_ImmutableBlob_asCPointerImpl")
@Escapes.Nothing // the usage site must guarantee that the receiver is kept alive long enough.
private external fun ImmutableBlob.asCPointerImpl(offset: Int): kotlin.native.internal.NativePtr

/**
 * Creates [ImmutableBlob] out of compile-time constant data.
 *
 * This method accepts values of [Short] type in range `0x00..0xff`, other values are prohibited.
 *
 * One element still represent one byte in the output data.
 * This is the only way to create ImmutableBlob for now.
 */
@Suppress("DEPRECATION_ERROR")
@Deprecated("ImmutableBlob is deprecated. Use ByteArray instead.", ReplaceWith("byteArrayOf(*elements)"))
@DeprecatedSinceKotlin(warningSince = "1.9", errorSince = "2.1")
@TypedIntrinsic(IntrinsicType.IMMUTABLE_BLOB)
@Escapes.Nothing
public external fun immutableBlobOf(vararg elements: Short): ImmutableBlob
