/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal.swiftExportRuntime

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.NativePtr
import kotlin.native.internal.concurrent.MemoryOrdering
import kotlin.native.internal.features.isSwiftExportEnabled

/**
 * Function pointer to convert Kotlin instances to retained Swift instances.
 *
 * Use [@ToRetainedSwift][kotlin.native.internal.ref.ToRetainedSwift] to associate a class with function that converts
 * Kotlin instances to Swift instances
 *
 * Use [x::class.toRetainedSwiftFunPtr][kotlin.reflect.KClass.toRetainedSwiftFunPtr] to get [ToRetainedSwiftFunPtr]
 * capable of converting `x` to Swift.
 */
@InternalForKotlinNative
public value class ToRetainedSwiftFunPtr internal constructor(private val typeInfo: NativePtr) {
    init {
        require(!typeInfo.isNull())
        require(isSwiftExportEnabled())
    }

    /**
     * Atomically load from [ToRetainedSwiftFunPtr] using [ordering].
     */
    public fun load(ordering: MemoryOrdering): NativePtr = loadToRetainedSwiftFunPtr(typeInfo, ordering.value)

    /**
     * Atomically store [value] in [ToRetainedSwiftFunPtr] using [ordering].
     */
    public fun store(
            value: NativePtr,
            ordering: MemoryOrdering,
    ): Unit = storeToRetainedSwiftFunPtr(typeInfo, value, ordering.value)

    /**
     * Perform atomic compare-and-exchange on [ToRetainedSwiftFunPtr].
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
    ): NativePtr = compareAndExchangeToRetainedSwiftFunPtr(typeInfo, expected, value, orderingSuccess.value, orderingFailure.value)
}

@GCUnsafeCall("Kotlin_native_internal_swiftExportRuntime_loadToRetainedSwiftFunPtr")
private external fun loadToRetainedSwiftFunPtr(typeInfo: NativePtr, ordering: Int): NativePtr

@GCUnsafeCall("Kotlin_native_internal_swiftExportRuntime_storeToRetainedSwiftFunPtr")
private external fun storeToRetainedSwiftFunPtr(typeInfo: NativePtr, value: NativePtr, ordering: Int)

@GCUnsafeCall("Kotlin_native_internal_swiftExportRuntime_compareAndExchangeToRetainedSwiftFunPtr")
private external fun compareAndExchangeToRetainedSwiftFunPtr(
        typeInfo: NativePtr,
        expected: NativePtr,
        value: NativePtr,
        orderingSuccess: Int,
        orderingFailure: Int,
): NativePtr