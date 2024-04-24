/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal.ref

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.Escapes
import kotlin.native.internal.GCUnsafeCall
import kotlin.native.internal.InternalForKotlinNative
import kotlin.native.internal.NativePtr

// TODO(KT-67741): Move all stdlib usages of special refs to this API (this API is allowed to grow to accommodate)

/**
 * An externally-reference-counted reference to a Kotlin object.
 *
 * It's similar to [StableRef][kotlinx.cinterop.StableRef] but additionally allows incrementing and decrementing the reference count.
 * When reference count is >0, acts like a strong reference, making sure the object cannot be collected by the GC.
 * When reference count is 0, acts like a weak reference, allowing the GC to collect the object. [tryRetainExternalRCRef] can then be used to try
 * turn this weak reference into a strong reference.
 * It's up to the API user to correctly track the current reference count.
 *
 * - [createRetainedExternalRCRef] creates a new [ExternalRCRef] with the reference count set to 1.
 * - [disposeExternalRCRef] disposes [ExternalRCRef] making it invalid. It may only be called when reference count is 0.
 * - [dereferenceExternalRCRef] get Kotlin object out of a valid [ExternalRCRef]. It may only be called when reference count is >0.
 * - [retainExternalRCRef] increments the reference count of a valid [ExternalRCRef]. It may only be called when reference count is >0.
 *   To increment when reference count is 0, see [tryRetainExternalRCRef].
 * - [releaseExternalRCRef] decrements the reference count of a valid [ExternalRCRef]. It may only be called when reference count is >0.
 * - [tryRetainExternalRCRef] tries to increment the reference count of a valid [ExternalRCRef]. When reference count is >0, acts like [retainExternalRCRef].
 *   When reference count is 0, increments it only if the GC has not yet collected the underlying object.
 *
 * NOTE: this API is very unsafe and is subject to change.
 */
@InternalForKotlinNative
public typealias ExternalRCRef = NativePtr

/**
 * Create a new [ExternalRCRef] for Kotlin object [obj] with the initial reference count of 1.
 */
@InternalForKotlinNative
@GCUnsafeCall("Kotlin_native_internal_ref_createRetainedExternalRCRef")
@Escapes(0b01) // obj is stored in the created ref.
public external fun createRetainedExternalRCRef(obj: Any): ExternalRCRef

/**
 * Dispose a valid [ExternalRCRef].
 *
 * [ref] becomes invalid to use after this operation.
 * May only be called if the reference count is 0. Otherwise, the behavior is undefined.
 */
@InternalForKotlinNative
@GCUnsafeCall("Kotlin_native_internal_ref_disposeExternalRCRef")
public external fun disposeExternalRCRef(ref: ExternalRCRef)

/**
 * Return the underlying object of this [ExternalRCRef].
 *
 * May only be called if the reference count is >0. Otherwise, the behavior is undefined.
 */
@InternalForKotlinNative
@GCUnsafeCall("Kotlin_native_internal_ref_dereferenceExternalRCRef")
public external fun dereferenceExternalRCRef(ref: ExternalRCRef): Any

/**
 * Increment the reference count of this [ExternalRCRef].
 *
 * Can be called concurrently with other retain/release operations.
 * May only be called if the reference count is >0. Otherwise, the behavior is undefined.
 *
 * @see tryRetainExternalRCRef
 */
@InternalForKotlinNative
@GCUnsafeCall("Kotlin_native_internal_ref_retainExternalRCRef")
public external fun retainExternalRCRef(ref: ExternalRCRef)

/**
 * Decrement the reference count of this [ExternalRCRef].
 *
 * Can be called concurrently with other retain/release operations.
 * May only be called if the reference count is >0. Otherwise, the behavior is undefined.
 */
@InternalForKotlinNative
@GCUnsafeCall("Kotlin_native_internal_ref_releaseExternalRCRef")
public external fun releaseExternalRCRef(ref: ExternalRCRef)

/**
 * Try to increment the reference count of this [ExternalRCRef].
 *
 * Can be called concurrently with other retain/release operations.
 * If the reference count is >0, works just like [retainExternalRCRef].
 * If the reference count is 0, will only increment the reference count if the underlying object is not yet collected by the GC.
 *
 * @return `true` if the increment was successful and `false` if the underlying object is already collected by the GC
 */
@InternalForKotlinNative
@GCUnsafeCall("Kotlin_native_internal_ref_tryRetainExternalRCRef")
public external fun tryRetainExternalRCRef(ref: ExternalRCRef): Boolean
