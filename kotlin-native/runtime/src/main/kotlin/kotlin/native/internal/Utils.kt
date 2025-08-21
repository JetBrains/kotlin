/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal

import kotlin.reflect.KClass
import kotlinx.cinterop.*
import kotlin.native.internal.escapeAnalysis.Escapes
import kotlin.native.internal.escapeAnalysis.PointsTo

@GCUnsafeCall("Kotlin_internal_reflect_getObjectReferenceFieldsCount")
@Escapes.Nothing
private external fun getObjectReferenceFieldsCount(o: Any): Int

@GCUnsafeCall("Kotlin_internal_reflect_getObjectReferenceFieldByIndex")
@PointsTo(0x000, 0x000, 0x002) // the return value references `o`'s contents.
private external fun getObjectReferenceFieldByIndex(o: Any, index: Int): Any?

/**
 * Returns [List] of non-null reference fields of the object.
 *
 * This function is intended to be used for testing and debugging purposes.
 * - It heavily relies on internal ABI details. No compatibility guarantees on exact list contents are provided.
 * - Order and representation of object's fields are subject to change.
 * - Performance characteristics of the implementation
 *
 * Limitations:
 *  - Primitives (unboxed [Int], [Double], [Float], etc.) are not included in the result.
 *  - Non-boxed value classes over primitives are not included in the result.
 *  - Non-boxed value classes over references are included in the result as the underlying reference type.
 *  - Synthetic fields (e.g. special fields for delegation) are included in the result.
 *  - There is no way to find which reference in the result corresponds to which field.
 *
 *  For `Array<T>` list of all its non-null elements is returned.
 *  For primitive arrays ([IntArray], [DoubleArray], [FloatArray], etc.) empty list is returned.
 */
@InternalForKotlinNative
public fun Any.collectReferenceFieldValues() : List<Any> {
    return when {
        this is Array<*> -> this.filterNotNull()
        else -> (0 until getObjectReferenceFieldsCount(this@collectReferenceFieldValues)).mapNotNull {
            getObjectReferenceFieldByIndex(this@collectReferenceFieldValues, it)
        }
    }
}
