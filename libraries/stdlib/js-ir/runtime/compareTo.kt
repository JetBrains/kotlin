/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js


// Adopted from misc.js

internal fun compareTo(a: dynamic, b: dynamic): Int = when (jsTypeOf(a)) {
    "number" -> when {
        jsTypeOf(b) == "number" ->
            doubleCompareTo(a, b)
        b is Long ->
            doubleCompareTo(a, b.toDouble())
        else ->
            primitiveCompareTo(a, b)
    }

    "string", "boolean" -> primitiveCompareTo(a, b)

    else -> compareToDoNotIntrinsicify(a, b)
}

@DoNotIntrinsify
private fun <T : Comparable<T>> compareToDoNotIntrinsicify(a: Comparable<T>, b: T) =
    a.compareTo(b)

internal fun primitiveCompareTo(a: dynamic, b: dynamic): Int =
    when {
        a < b -> -1
        a > b -> 1
        else -> 0
    }

internal fun doubleCompareTo(a: dynamic, b: dynamic): Int =
    when {
        a < b -> -1
        a > b -> 1

        a === b -> {
            if (a !== 0)
                0
            else {
                val ia = 1.asDynamic() / a
                if (ia === 1.asDynamic() / b) {
                    0
                } else if (ia < 0) {
                    -1
                } else {
                    1
                }
            }
        }

        a !== a ->
            if (b !== b) 0 else 1

        else -> -1
    }