/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native.ref

import kotlinx.cinterop.*
import kotlin.native.internal.*

/**
 *   Theory of operations:
 *
 *  Weak references in Kotlin/Native are implemented in the following way. Whenever weak reference to a
 * object is created, we try to create one of `WeakReferenceImpl`:
 * `PermanentWeakReferenceImpl` for permanent objects,
 * `ObjCWeakReferenceImpl` for objects coming from ObjC,
 * or `RegularWeakReferenceImpl` for all other Kotlin objects.
 * The latter are stored in object's extra data, so only one per object is created.
 * Every other weak reference contains a strong reference to the impl object.
 *
 *              [weak1]   [weak2]
 *                 \       /
 *                  V     V
 *     ... [RegularWeakReferenceImpl] <-
 *     .                                |
 *     .                                |
 *      ->[Object] -> [ExtraObjectData]-
 *
 * References from weak reference objects to the `RegularWeakReferenceImpl` and from the
 * extra data to the `RegularWeakReferenceImpl` are strong, and from the
 * `RegularWeakReferenceImpl` to the object is nullably weak. During GC if the object is
 * considered dead, the reference inside `RegularWeakReferenceImpl` is nulled out.
 */

// Clear holding the counter object, which refers to the actual object.
@NoReorderFields
@Frozen
@OptIn(FreezingIsDeprecated::class)
internal class WeakReferenceCounterLegacyMM(var referred: COpaquePointer?) : WeakReferenceImpl() {
    // Spinlock, potentially taken when materializing or removing 'referred' object.
    var lock: Int = 0

    // Optimization for concurrent access.
    var cookie: Int = 0

    @GCUnsafeCall("Konan_WeakReferenceCounterLegacyMM_get")
    external override fun get(): Any?
}

@NoReorderFields
@ExportTypeInfo("theRegularWeakReferenceImplTypeInfo")
@HasFinalizer // TODO: Consider just using Cleaners.
internal class RegularWeakReferenceImpl(
    val weakRef: COpaquePointer,
    val referred: COpaquePointer, // TODO: This exists only for the ExtraObjectData's sake. Refactor and remove.
) : WeakReferenceImpl() {
    @GCUnsafeCall("Konan_RegularWeakReferenceImpl_get")
    external override fun get(): Any?
}

@PublishedApi
internal abstract class WeakReferenceImpl {
    abstract fun get(): Any?
}

// Get a counter from non-null object.
@GCUnsafeCall("Konan_getWeakReferenceImpl")
@Escapes(0b01) // referent escapes.
external internal fun getWeakReferenceImpl(referent: Any): WeakReferenceImpl

// Create a counter object for legacy MM.
@ExportForCppRuntime
internal fun makeWeakReferenceCounterLegacyMM(referred: COpaquePointer) = WeakReferenceCounterLegacyMM(referred)

// Create a counter object.
@ExportForCppRuntime
internal fun makeRegularWeakReferenceImpl(weakRef: COpaquePointer, referred: COpaquePointer) = RegularWeakReferenceImpl(weakRef, referred)

internal class PermanentWeakReferenceImpl(val referred: Any): kotlin.native.ref.WeakReferenceImpl() {
    override fun get(): Any? = referred
}

// Create a reference to the permanent object.
@ExportForCppRuntime
internal fun makePermanentWeakReferenceImpl(referred: Any) = PermanentWeakReferenceImpl(referred)
