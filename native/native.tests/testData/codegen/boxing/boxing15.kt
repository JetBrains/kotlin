// OUTPUT_DATA_FILE: boxing15.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    println(foo(17))
    val nonConst = 17
    println(foo(nonConst))

    return "OK"
}

fun <T : Int> foo(x: T): Int = x
