/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js


// Adopted from misc.js

fun compareTo(a: dynamic, b: dynamic): Int = when (typeOf(a)) {
    "number" -> when {
        typeOf(b) == "number" ->
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

fun primitiveCompareTo(a: dynamic, b: dynamic): Int =
    js("a < b ? -1 : a > b ? 1 : 0").unsafeCast<Int>()

fun doubleCompareTo(a: dynamic, b: dynamic): Int =
    js("""
    if (a < b) return -1;
    if (a > b) return 1;

    if (a === b) {
        if (a !== 0) return 0;

        var ia = 1 / a;
        return ia === 1 / b ? 0 : (ia < 0 ? -1 : 1);
    }

    return a !== a ? (b !== b ? 0 : 1) : -1
    """).unsafeCast<Int>()