/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package a

fun IntArray.forEachNoInline(block: (Int) -> Unit) = this.forEach { block(it) }

inline fun fold(initial: Int, values: IntArray, crossinline block: (Int, Int) -> Int): Int {
    var res = initial
    values.forEachNoInline {
        res = block(res, it)
    }
    return res
}