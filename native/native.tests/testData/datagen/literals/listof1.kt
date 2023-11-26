/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// OUTPUT_DATA_FILE: listof1.out
// IGNORE_BACKEND: NATIVE

import kotlin.test.*

fun box(): String {
    val list = foo()
    println(list === foo())
    println(list.toString())

    return "OK"
}

fun foo(): List<String> {
    return listOf("a", "b", "c")
}