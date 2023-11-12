/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlinx.cinterop

import kotlin.native.internal.reinterpret
import kotlin.native.internal.Intrinsic
import kotlin.native.internal.VolatileLambda
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

@ExperimentalForeignApi
public typealias NativePtr = kotlin.native.internal.NativePtr
internal typealias NonNullNativePtr = kotlin.native.internal.NonNullNativePtr

@Suppress("NOTHING_TO_INLINE")
@ExperimentalForeignApi
internal inline fun NativePtr.toNonNull() = this.reinterpret<NativePtr, NonNullNativePtr>()

@ExperimentalForeignApi
public inline val nativeNullPtr: NativePtr
    get() = NativePtr.NULL

@Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
@Suppress("DEPRECATION")
@ExperimentalForeignApi
public fun <T : CVariable> typeOf(): CVariable.Type = throw Error("typeOf() is called with erased argument")

/**
 * Performs type cast of the native pointer to given interop type, including null values.
 *
 * @param T must not be abstract
 */
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.IDENTITY)
public external fun <T : NativePointed> interpretNullablePointed(ptr: NativePtr): T?

/**
 *  Performs type cast of the [CPointer] from the given raw pointer.
 */
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.IDENTITY)
public external fun <T : CPointed> interpretCPointer(rawValue: NativePtr): CPointer<T>?

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.IDENTITY)
public external fun NativePointed.getRawPointer(): NativePtr

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.IDENTITY)
public external fun CPointer<*>.getRawValue(): NativePtr

@ExperimentalForeignApi
internal fun CPointer<*>.cPointerToString() = "CPointer(raw=$rawValue)"

/**
 * Returns a pointer to C function which calls given Kotlin *static* function.
 *
 * @param function must be *static*, i.e. an (unbound) reference to a Kotlin function or
 * a closure which doesn't capture any variable
 */
@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <R> staticCFunction(@VolatileLambda function: () -> R): CPointer<CFunction<() -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, R> staticCFunction(@VolatileLambda function: (P1) -> R): CPointer<CFunction<(P1) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, R> staticCFunction(@VolatileLambda function: (P1, P2) -> R): CPointer<CFunction<(P1, P2) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, R> staticCFunction(@VolatileLambda function: (P1, P2, P3) -> R): CPointer<CFunction<(P1, P2, P3) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4) -> R): CPointer<CFunction<(P1, P2, P3, P4) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R>>

@ExperimentalForeignApi
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION)
public external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R>>
