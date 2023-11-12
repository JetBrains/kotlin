/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlinx.cinterop

import kotlin.native.*
import kotlin.native.internal.GCUnsafeCall

@ExperimentalForeignApi
public data class Pinned<out T : Any> internal constructor(private val stablePtr: COpaquePointer) {

    /**
     * Disposes the handle. It must not be [used][get] after that.
     */
    public fun unpin() {
        disposeStablePointer(this.stablePtr)
    }

    /**
     * Returns the underlying pinned object.
     */
    public fun get(): T = @Suppress("UNCHECKED_CAST") (derefStablePointer(stablePtr) as T)

}

@ExperimentalForeignApi
public fun <T : Any> T.pin(): Pinned<T> = Pinned<T>(createStablePointer(this))

@ExperimentalForeignApi
public inline fun <T : Any, R> T.usePinned(block: (Pinned<T>) -> R): R {
    val pinned = this.pin()
    return try {
        block(pinned)
    } finally {
        pinned.unpin()
    }
}

@ExperimentalForeignApi
public fun Pinned<ByteArray>.addressOf(index: Int): CPointer<ByteVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun ByteArray.refTo(index: Int): CValuesRef<ByteVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<String>.addressOf(index: Int): CPointer<COpaque> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun String.refTo(index: Int): CValuesRef<COpaque> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<CharArray>.addressOf(index: Int): CPointer<COpaque> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun CharArray.refTo(index: Int): CValuesRef<COpaque> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<ShortArray>.addressOf(index: Int): CPointer<ShortVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun ShortArray.refTo(index: Int): CValuesRef<ShortVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<IntArray>.addressOf(index: Int): CPointer<IntVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun IntArray.refTo(index: Int): CValuesRef<IntVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<LongArray>.addressOf(index: Int): CPointer<LongVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun LongArray.refTo(index: Int): CValuesRef<LongVar> = this.usingPinned { addressOf(index) }

// TODO: pinning of unsigned arrays involves boxing as they are inline classes wrapping signed arrays.
@ExperimentalForeignApi
public fun Pinned<UByteArray>.addressOf(index: Int): CPointer<UByteVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun UByteArray.refTo(index: Int): CValuesRef<UByteVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<UShortArray>.addressOf(index: Int): CPointer<UShortVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun UShortArray.refTo(index: Int): CValuesRef<UShortVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<UIntArray>.addressOf(index: Int): CPointer<UIntVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun UIntArray.refTo(index: Int): CValuesRef<UIntVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<ULongArray>.addressOf(index: Int): CPointer<ULongVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun ULongArray.refTo(index: Int): CValuesRef<ULongVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<FloatArray>.addressOf(index: Int): CPointer<FloatVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun FloatArray.refTo(index: Int): CValuesRef<FloatVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
public fun Pinned<DoubleArray>.addressOf(index: Int): CPointer<DoubleVar> = this.get().addressOfElement(index)
@ExperimentalForeignApi
public fun DoubleArray.refTo(index: Int): CValuesRef<DoubleVar> = this.usingPinned { addressOf(index) }

@ExperimentalForeignApi
private inline fun <T : Any, P : CPointed> T.usingPinned(
        crossinline block: Pinned<T>.() -> CPointer<P>
) = object : CValuesRef<P>() {

    override fun getPointer(scope: AutofreeScope): CPointer<P> {
        val pinned = this@usingPinned.pin()
        scope.defer { pinned.unpin() }
        return pinned.block()
    }
}

@GCUnsafeCall("Kotlin_Arrays_getByteArrayAddressOfElement")
private external fun ByteArray.addressOfElement(index: Int): CPointer<ByteVar>

@GCUnsafeCall("Kotlin_Arrays_getStringAddressOfElement")
private external fun String.addressOfElement(index: Int): CPointer<COpaque>

@GCUnsafeCall("Kotlin_Arrays_getCharArrayAddressOfElement")
private external fun CharArray.addressOfElement(index: Int): CPointer<COpaque>

@GCUnsafeCall("Kotlin_Arrays_getShortArrayAddressOfElement")
private external fun ShortArray.addressOfElement(index: Int): CPointer<ShortVar>

@GCUnsafeCall("Kotlin_Arrays_getIntArrayAddressOfElement")
private external fun IntArray.addressOfElement(index: Int): CPointer<IntVar>

@GCUnsafeCall("Kotlin_Arrays_getLongArrayAddressOfElement")
private external fun LongArray.addressOfElement(index: Int): CPointer<LongVar>

@GCUnsafeCall("Kotlin_Arrays_getByteArrayAddressOfElement")
private external fun UByteArray.addressOfElement(index: Int): CPointer<UByteVar>

@GCUnsafeCall("Kotlin_Arrays_getShortArrayAddressOfElement")
private external fun UShortArray.addressOfElement(index: Int): CPointer<UShortVar>

@GCUnsafeCall("Kotlin_Arrays_getIntArrayAddressOfElement")
private external fun UIntArray.addressOfElement(index: Int): CPointer<UIntVar>

@GCUnsafeCall("Kotlin_Arrays_getLongArrayAddressOfElement")
private external fun ULongArray.addressOfElement(index: Int): CPointer<ULongVar>

@GCUnsafeCall("Kotlin_Arrays_getFloatArrayAddressOfElement")
private external fun FloatArray.addressOfElement(index: Int): CPointer<FloatVar>

@GCUnsafeCall("Kotlin_Arrays_getDoubleArrayAddressOfElement")
private external fun DoubleArray.addressOfElement(index: Int): CPointer<DoubleVar>
