// OUTPUT_DATA_FILE: concatenation.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    val s = "world"
    val i = 1
    println("Hello $s $i ${2*i}")

    for (item in listOf("a", "b")) {
        println("Hello, $item")
    }

    return "OK"
}
