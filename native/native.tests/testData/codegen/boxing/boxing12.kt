// OUTPUT_DATA_FILE: boxing12.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun foo(x: Number) {
    println(x.toByte())
}

fun box(): String {
    foo(18)
    val nonConst = 18
    foo(nonConst)

    return "OK"
}
