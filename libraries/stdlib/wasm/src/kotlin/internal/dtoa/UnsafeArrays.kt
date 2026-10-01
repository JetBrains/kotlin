/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal.dtoa

import kotlin.wasm.internal.copyWasmArray

internal actual fun longArrayGetUnsafe(array: LongArray, index: Int): Long =
    array.storage.get(index)

internal actual fun longArraySetUnsafe(array: LongArray, index: Int, value: Long): Unit =
    array.storage.set(index, value)

internal actual fun doubleArrayGetUnsafe(array: DoubleArray, index: Int): Double =
    array.storage.get(index)

internal actual fun doubleArraySetUnsafe(array: DoubleArray, index: Int, value: Double): Unit =
    array.storage.set(index, value)

internal actual fun intArrayGetUnsafe(array: IntArray, index: Int): Int =
    array.storage.get(index)

internal actual fun intArraySetUnsafe(array: IntArray, index: Int, value: Int): Unit =
    array.storage.set(index, value)

internal actual fun copyIntoUnsafe(source: LongArray, destination: LongArray, destinationOffset: Int, startIndex: Int, endIndex: Int) {
    copyWasmArray(source.storage, destination.storage, startIndex, destinationOffset, endIndex - startIndex)
}
