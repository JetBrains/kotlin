/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js


// Adopted from misc.js

fun compareTo(a: dynamic, b: dynamic): Int {
    var typeA = typeOf(a)
    if (typeA == "number") {
        if (typeOf(b) == "number") {
            return doubleCompareTo(a, b)
        }
        return primitiveCompareTo(a, b)
    }
    if (typeA == "string" || typeA == "boolean") {
        return primitiveCompareTo(a, b)
    }

    // TODO: Replace to a.unsafeCast<Comparable<*>>().compareTo(b) when bridge is implemented
    return js("a.compareTo(b)").unsafeCast<Int>()
}

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