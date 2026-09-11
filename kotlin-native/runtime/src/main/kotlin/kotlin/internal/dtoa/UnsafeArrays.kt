/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal.dtoa

import kotlin.native.internal.*
import kotlin.native.internal.escapeAnalysis.Escapes

@GCUnsafeCall("Kotlin_LongArray_get_without_BoundCheck")
@Escapes.Nothing
internal actual external fun longArrayGetUnsafe(array: LongArray, index: Int): Long

@GCUnsafeCall("Kotlin_LongArray_set_without_BoundCheck")
@Escapes.Nothing
internal actual external fun longArraySetUnsafe(array: LongArray, index: Int, value: Long)

@GCUnsafeCall("Kotlin_DoubleArray_get_without_BoundCheck")
@Escapes.Nothing
internal actual external fun doubleArrayGetUnsafe(array: DoubleArray, index: Int): Double

@GCUnsafeCall("Kotlin_DoubleArray_set_without_BoundCheck")
@Escapes.Nothing
internal actual external fun doubleArraySetUnsafe(array: DoubleArray, index: Int, value: Double)

@GCUnsafeCall("Kotlin_IntArray_get_without_BoundCheck")
@Escapes.Nothing
internal actual external fun intArrayGetUnsafe(array: IntArray, index: Int): Int

@GCUnsafeCall("Kotlin_IntArray_set_without_BoundCheck")
@Escapes.Nothing
internal actual external fun intArraySetUnsafe(array: IntArray, index: Int, value: Int)

internal actual fun copyIntoUnsafe(source: LongArray, destination: LongArray, destinationOffset: Int, startIndex: Int, endIndex: Int) {
    source.copyInto(destination, destinationOffset, startIndex, endIndex)
}