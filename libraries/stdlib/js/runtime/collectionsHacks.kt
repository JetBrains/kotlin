/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.js.*

internal fun arrayToString(array: Array<*>) = array.joinToString(", ", "[", "]") { toString(it) }

@OptIn(ExperimentalUnsignedTypes::class)
internal fun <T> Array<out T>?.contentDeepHashCodeInternal(): Int {
    if (this == null) return 0
    var result = 1
    for (element in this) {
        val elementHash = when {
            element == null -> 0
            isArrayish(element) -> (element.unsafeCast<Array<*>>()).contentDeepHashCodeInternal()

            element is UByteArray   -> element.contentHashCode()
            element is UShortArray  -> element.contentHashCode()
            element is UIntArray    -> element.contentHashCode()
            element is ULongArray   -> element.contentHashCode()

            else                    -> element.hashCode()
        }

        result = 31 * result + elementHash
    }
    return result
}

internal fun <T> T.contentEqualsInternal(other: T): Boolean {
    val a = this.asDynamic()
    val b = other.asDynamic()

    if (a === b) return true

    if (a == null || b == null || !isArrayish(b) || a.length != b.length) return false

    for (i in 0 until a.length) {
        if (!equals(a[i], b[i])) {
            return false
        }
    }
    return true
}

internal fun <T> T.contentHashCodeInternal(): Int {
    val a = this.asDynamic()
    if (a == null) return 0

    var result = 1

    for (i in 0 until a.length) {
        result = result * 31 + hashCode(a[i])
    }

    return result
}