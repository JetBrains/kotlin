/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// OUTPUT_DATA_FILE: strdedup2.out
// TODO: string deduplication across several components seems to require
// linking them as bitcode modules before translating to machine code.
// IGNORE_BACKEND: NATIVE

import kotlin.test.*

fun box(): String {
    val str1 = ""
    val str2 = "hello".subSequence(2, 2)
    println(str1 == str2)
    println(str1 === str2)

    return "OK"
}
