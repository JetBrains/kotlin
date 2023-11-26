// OUTPUT_DATA_FILE: boxing3.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Int?) {
    if (arg != null)
        printInt(arg)
}

fun box(): String {
    foo(42)
    val nonConst = 42
    foo(nonConst)

    return "OK"
}
