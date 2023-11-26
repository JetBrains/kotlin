// OUTPUT_DATA_FILE: boxing5.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Int?) {
    printInt(arg ?: 16)
}

fun box(): String {
    foo(null)
    foo(42)
    val nonConstNull = null
    val nonConstInt = 42
    foo(nonConstNull)
    foo(nonConstInt)

    return "OK"
}
