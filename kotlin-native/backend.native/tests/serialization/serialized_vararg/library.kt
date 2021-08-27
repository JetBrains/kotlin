/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

fun bar(vararg x: Int) {
    x.forEach {
        println(it)
    }
    println("size: ${x.size}")
}

inline fun foo() = bar(17, 19, 23, *intArrayOf(29, 31))

