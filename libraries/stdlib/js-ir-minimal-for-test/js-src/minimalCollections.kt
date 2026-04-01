/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

import kotlin.internal.UsedFromCompilerGeneratedCode

@Suppress("UNUSED_PARAMETER")
@SinceKotlin("1.4")
@library("arrayEquals")
public infix fun <T> Array<out T>?.contentEquals(other: Array<out T>?): Boolean {
    definedExternally
}

@SinceKotlin("1.4")
public fun IntArray?.contentToString(): String {
    TODO("Not implemented in reduced runtime")
}

@SinceKotlin("1.4")
public fun LongArray?.contentToString(): String {
    TODO("Not implemented in reduced runtime")
}

@SinceKotlin("1.4")
public fun <T> Array<out T>?.contentToString(): String {
    TODO("Not implemented in reduced runtime")
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