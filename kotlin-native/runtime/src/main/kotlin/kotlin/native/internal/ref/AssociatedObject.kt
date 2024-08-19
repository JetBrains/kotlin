/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal.ref

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.NativePtr
import kotlin.native.internal.concurrent.MemoryOrdering
import kotlin.native.internal.features.isObjCInteropEnabled

/**
 * Associated ObjC object.
 *
 * Use [x.associatedObject][Any.associatedObject] to get [AssociatedObject] for given Kotlin object `x`.
 */
@InternalForKotlinNative
public value class AssociatedObject internal constructor(private val ref: Any) {
    init {
        require(isObjCInteropEnabled())
    }

    /**
     * Atomically load from [AssociatedObject] using [ordering].
     */
    public fun load(ordering: MemoryOrdering): NativePtr = loadAssociatedObject(ref, ordering.value)

    /**
     * Atomically store [value] in [AssociatedObject] using [ordering].
     */
    public fun store(
            value: NativePtr,
            ordering: MemoryOrdering,
    ): Unit = storeAssociatedObject(ref, value, ordering.value)

    /**
     * Perform atomic compare-and-exchange on [AssociatedObject].
     *
     * Compares current value `x` with [expected].
     * If it matches, atomically replaces it with [value] using [orderingSuccess] and returns [expected].
     * If it doesn't match, returns `x` as if loaded using [orderingFailure].
     */
    public fun compareAndExchange(
            expected: NativePtr,
            value: NativePtr,
            orderingSuccess: MemoryOrdering,
            orderingFailure: MemoryOrdering,
    ): NativePtr = compareAndExchangeAssociatedObject(ref, expected, value, orderingSuccess.value, orderingFailure.value)
}

@GCUnsafeCall("Kotlin_native_internal_ref_loadAssociatedObject")
private external fun loadAssociatedObject(ref: Any, ordering: Int): NativePtr

@GCUnsafeCall("Kotlin_native_internal_ref_storeAssociatedObject")
private external fun storeAssociatedObject(ref: Any, value: NativePtr, ordering: Int)

@GCUnsafeCall("Kotlin_native_internal_ref_compareAndExchangeAssociatedObject")
private external fun compareAndExchangeAssociatedObject(
        ref: Any,
        expected: NativePtr,
        value: NativePtr,
        orderingSuccess: Int,
        orderingFailure: Int,
): NativePtr