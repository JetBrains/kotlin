/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

internal fun <T> Array<T>.resetAt(index: Int) {
    this.unsafeCast<Array<Any?>>()[index] = null
}

internal fun <T> Array<T>.resetRange(fromIndex: Int, toIndex: Int) {
    this.nativeFill(null, fromIndex, toIndex)
}

internal fun <T> Array<T>.copyOfUninitializedElements(newSize: Int): Array<T> {
    return this.copyOf(newSize).unsafeCast<Array<T>>()
}

internal fun <T> arrayOfUninitializedElements(capacity: Int): Array<T> {
    require(capacity >= 0) { "capacity must be non-negative." }
    return arrayOfNulls<Any>(capacity).unsafeCast<Array<T>>()
}
