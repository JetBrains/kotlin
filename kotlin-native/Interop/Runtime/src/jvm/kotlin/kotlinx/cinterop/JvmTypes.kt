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

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.companionObjectInstance

typealias NativePtr = Long
internal typealias NonNullNativePtr = NativePtr
@PublishedApi internal fun NonNullNativePtr.toNativePtr() = this
internal fun NativePtr.toNonNull(): NonNullNativePtr = this

public val nativeNullPtr: NativePtr = 0L

// TODO: the functions below should eventually be intrinsified

@Suppress("DEPRECATION")
private val typeOfCache = ConcurrentHashMap<Class<*>, CVariable.Type>()

@Deprecated("Use sizeOf<T>() or alignOf<T>() instead.")
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
inline fun <reified T : CVariable> typeOf() =
        @Suppress("DEPRECATION")
        typeOfCache.computeIfAbsent(T::class.java) { T::class.companionObjectInstance as CVariable.Type }

/**
 * Returns interpretation of entity with given pointer, or `null` if it is null.
 *
 * @param T must not be abstract
 */
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
inline fun <reified T : NativePointed> interpretNullablePointed(ptr: NativePtr): T? {
    if (ptr == nativeNullPtr) {
        return null
    } else {
        val result = nativeMemUtils.allocateInstance<T>()
        result.rawPtr = ptr
        return result
    }
}

/**
 * Creates a [CPointer] from the raw pointer of [NativePtr].
 *
 * @return a [CPointer] representation, or `null` if the [rawValue] represents native `nullptr`.
 */
fun <T : CPointed> interpretCPointer(rawValue: NativePtr) =
        if (rawValue == nativeNullPtr) {
            null
        } else {
            CPointer<T>(rawValue)
        }

internal fun CPointer<*>.cPointerToString() = "CPointer(raw=0x%x)".format(rawValue)

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class CLength(val value: Int)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CNaturalStruct(vararg val fieldNames: String)

fun <R> staticCFunction(function: () -> R): CPointer<CFunction<() -> R>> =
        staticCFunctionImpl(function)

fun <P1, R> staticCFunction(function: (P1) -> R): CPointer<CFunction<(P1) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, R> staticCFunction(function: (P1, P2) -> R): CPointer<CFunction<(P1, P2) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, R> staticCFunction(function: (P1, P2, P3) -> R): CPointer<CFunction<(P1, P2, P3) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, R> staticCFunction(function: (P1, P2, P3, P4) -> R): CPointer<CFunction<(P1, P2, P3, P4) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, R> staticCFunction(function: (P1, P2, P3, P4, P5) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) -> R>> =
        staticCFunctionImpl(function)

fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> staticCFunction(function: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R): CPointer<CFunction<(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) -> R>> =
        staticCFunctionImpl(function)
