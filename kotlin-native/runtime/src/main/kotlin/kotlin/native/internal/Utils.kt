/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlin.native.internal

import kotlin.reflect.KClass
import kotlinx.cinterop.*

@ExportForCppRuntime
internal fun DescribeObjectForDebugging(typeInfo: NativePtr, address: NativePtr): String {
    val kClass = kotlin.native.internal.KClassImpl<Any>(typeInfo)
    return debugDescription(kClass, address.toLong().toInt())
}

internal fun debugDescription(kClass: KClass<*>, identity: Int): String {
    val className = kClass.qualifiedName ?: kClass.simpleName ?: "<object>"
    val unsignedIdentity = identity.toLong() and 0xffffffffL
    val identityStr = unsignedIdentity.toString(16)
    return "$className@$identityStr"
}

@GCUnsafeCall("Kotlin_internal_reflect_getObjectReferenceFieldsCount")
private external fun getObjectReferenceFieldsCount(o: Any): Int
@GCUnsafeCall("Kotlin_internal_reflect_getObjectReferenceFieldByIndex")
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
