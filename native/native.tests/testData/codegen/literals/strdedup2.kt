/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TODO: string deduplication across several components seems to require
// linking them as bitcode modules before translating to machine code.
// IGNORE_BACKEND: JVM, JVM_IR
// DISABLE_NATIVE
// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val str1 = ""
    val str2 = "hello".subSequence(2, 2)
    if(!(str1 == str2))
        return "FAIL =="
    if(!(str1 === str2))
        return "FAIL ==="

    return "OK"
}
