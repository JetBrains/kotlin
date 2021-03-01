/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package a

fun IntArray.forEachNoInline(block: (Int) -> Unit) = this.forEach { block(it) }

inline fun foo(values: IntArray, crossinline block: (Int, Int, Int) -> Int): Int {
    val o = object {
        lateinit var s: String
        var x: Int = 42
    }
    values.forEachNoInline {
        o.x = block(o.x, o.s.length, it)
    }
    return o.x
}