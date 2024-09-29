/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend

import org.jetbrains.kotlin.ir.declarations.*

/**
 * Represents an atomic handler provided for avery atomicfu property.
 * All atomic handlers come in two flavors:
 * 1. An atomic handler corresponding to the actual property, e.g.
 * ```
 * private val arr = kotlin.concurrent.AtomicIntArray(10) // this is an [AtomicArray] handler, which replaced the original atomicfu array.
 * arr.compareAndSet(0, 0, 100) // here an operation was invoked directly on the handler
 * ```
 * 2. An atomic handler corresponding to an argument passed to the extension function.
 *  These atomic handlers were added as separate classes because they hold an IrValueParameter, not an actual IrProperty,
 *  and atomic function invocation on the atomic handler passed as a value argument is processed differently, e.g.:
 *
 * ```
 * // original function
 * fun kotlinx.atomicfu.AtomicInt.foo(arg: Int) { compareAndSet(value, 56) }
 *
 * // transformed function for an array element receiver:
 * fun foo$atomicfu(atomicHandler: kotlin.concurrent.AtomicIntArray, index: Int, arg: Int) {
 *      atomicHandler.compareAndSet(index, value, 56) // here an operation was invoked on the atomic handler, which was passed as an argument.
 * }
 * // transformed function for a property reference receiver:
 * fun foo$atomicfu(refGetter: () -> KMutableProperty<Int>, arg: Int) {
 *     refGetter().compareAndSetField(value, 56)
 * }
 * ```
 */
sealed class AtomicHandler<T : IrDeclaration>(val declaration: T, val type: AtomicHandlerType)

/**
 * Atomic handlers for atomicfu properties on K/N.
 * [VolatilePropertyReference] contains a Volatile property reference, which is used to invoke atomic intrinsics (from kotlin.concurrent package).
 * [VolatilePropertyReferenceGetterValueParameter] is used to pass a Volatile property reference getter as an argument to the function.
 * This is necessary as we can only invoke atomic intrinsics on property references known at compile time.
 *
 *  fun foo$atomicfu(atomicHandler: () -> KMutableProperty<Int>, arg: Int) {
 *        atomicHandler()::compareAndSetField(value, 56) // here an operation was invoked on the atomic handler, which was passed as an argument.
 *   }
 */
class VolatilePropertyReference(property: IrProperty) : AtomicHandler<IrProperty>(property, AtomicHandlerType.NATIVE_PROPERTY_REF)
class VolatilePropertyReferenceGetterValueParameter(valueParameter: IrValueParameter) : AtomicHandler<IrValueParameter>(valueParameter, AtomicHandlerType.NATIVE_PROPERTY_REF)

/**
 * Atomic handlers for in-class atomicfu properties on JVM, containing Java atomic field updaters.
 */
class AtomicFieldUpdater(val volatileProperty: VolatilePropertyReference, property: IrProperty) : AtomicHandler<IrProperty>(property, AtomicHandlerType.ATOMIC_FIELD_UPDATER)
class AtomicFieldUpdaterValueParameter(valueParameter: IrValueParameter) : AtomicHandler<IrValueParameter>(valueParameter, AtomicHandlerType.ATOMIC_FIELD_UPDATER)

/**
 * Atomic handlers for top-level atomicfu properties on JVM, containing Java boxed atomics.
 */
class BoxedAtomic(property: IrProperty) : AtomicHandler<IrProperty>(property, AtomicHandlerType.BOXED_ATOMIC)
class BoxedAtomicValueParameter(valueParameter: IrValueParameter) : AtomicHandler<IrValueParameter>(valueParameter, AtomicHandlerType.BOXED_ATOMIC)

/**
 * Atomic handlers for atomicfu arrays, which contain Java or Kotlin Native atomic arrays, depending on the target platform.
 */
class AtomicArray(property: IrProperty) : AtomicHandler<IrProperty>(property, AtomicHandlerType.ATOMIC_ARRAY)
class AtomicArrayValueParameter(valueParameter: IrValueParameter) : AtomicHandler<IrValueParameter>(valueParameter, AtomicHandlerType.ATOMIC_ARRAY)

enum class AtomicHandlerType { ATOMIC_FIELD_UPDATER, BOXED_ATOMIC, ATOMIC_ARRAY, NATIVE_PROPERTY_REF }