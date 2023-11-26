// OUTPUT_DATA_FILE: boxing4.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Any?) {
    if (arg is Int? && arg != null)
        printInt(arg)
}

fun box(): String {
    foo(16)
    val nonConst = 16
    foo(nonConst)

    return "OK"
}
