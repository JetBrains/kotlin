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

package kotlinx.cinterop

import kotlin.native.internal.getNativeNullPtr
import kotlin.native.internal.reinterpret
import kotlin.native.internal.Intrinsic
import kotlin.native.internal.VolatileLambda
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

typealias NativePtr = kotlin.native.internal.NativePtr
internal typealias NonNullNativePtr = kotlin.native.internal.NonNullNativePtr

@Suppress("NOTHING_TO_INLINE")
internal inline fun NativePtr.toNonNull() = this.reinterpret<NativePtr, NonNullNativePtr>()

inline val nativeNullPtr: NativePtr
    get() = getNativeNullPtr()

@Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
@Suppress("DEPRECATION")
fun <T : CVariable> typeOf(): CVariable.Type = throw Error("typeOf() is called with erased argument")

/**
 * Performs type cast of the native pointer to given interop type, including null values.
 *
 * @param T must not be abstract
 */
@TypedIntrinsic(IntrinsicType.IDENTITY)
external fun <T : NativePointed> interpretNullablePointed(ptr: NativePtr): T?

/**
 *  Performs type cast of the [CPointer] from the given raw pointer.
 */
@TypedIntrinsic(IntrinsicType.IDENTITY)
external fun <T : CPointed> interpretCPointer(rawValue: NativePtr): CPointer<T>?

@TypedIntrinsic(IntrinsicType.IDENTITY)
external fun NativePointed.getRawPointer(): NativePtr

@TypedIntrinsic(IntrinsicType.IDENTITY)
external fun CPointer<*>.getRawValue(): NativePtr

internal fun CPointer<*>.cPointerToString() = "CPointer(raw=$rawValue)"

public class Vector128VarOf<T : Vector128>(rawPtr: NativePtr) : CVariable(rawPtr) {
    @Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
    @Suppress("DEPRECATION")
    companion object : Type(size = 16, align = 16)
}

public typealias Vector128Var = Vector128VarOf<Vector128>

public var <T : Vector128> Vector128VarOf<T>.value: T
    get() = nativeMemUtils.getVector(this) as T
    set(value) = nativeMemUtils.putVector(this, value)

/**
 * Returns a pointer to C function which calls given Kotlin *static* function.
 *
 * @param function must be *static*, i.e. an (unbound) reference to a Kotlin function or
 * a closure which doesn't capture any variable
 */
@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <R> staticCFunction(@VolatileLambda function: () -> R): CPointer<CFunction<() -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, R> staticCFunction(@VolatileLambda function: (P1) -> R): CPointer<CFunction<(P1) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, R> staticCFunction(@VolatileLambda function: (P1, P2) -> R): CPointer<CFunction<(P1, P2) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, R> staticCFunction(@VolatileLambda function: (P1, P2, P3) -> R): CPointer<CFunction<(P1, P2, P3) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4) -> R): CPointer<CFunction<(P1, P2, P3, P4) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R>>

@TypedIntrinsic(IntrinsicType.INTEROP_STATIC_C_FUNCTION) external fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> staticCFunction(@VolatileLambda function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R>>
