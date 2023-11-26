// OUTPUT_DATA_FILE: inline7.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun foo(vararg args: Int) {
    for (a in args) {
        println(a.toString())
    }
}

fun bar() {
    foo(1, 2, 3)
}

fun box(): String {
    bar()

    return "OK"
}
