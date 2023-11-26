// OUTPUT_DATA_FILE: strdedup1.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val str1 = "Hello"
    val str2 = "Hello"
    println(str1 == str2)
    println(str1 === str2)

    return "OK"
}
