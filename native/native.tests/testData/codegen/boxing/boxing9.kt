// OUTPUT_DATA_FILE: boxing9.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun foo(vararg args: Any?) {
    for (arg in args) {
        println(arg.toString())
    }
}

fun bar(vararg args: Any?) {
    foo(1, *args, 2, *args, 3)
}

fun box(): String {
    bar(null, true, "Hello")

    return "OK"
}
