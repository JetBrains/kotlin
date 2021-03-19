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
