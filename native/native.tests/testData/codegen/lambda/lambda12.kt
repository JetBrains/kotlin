// OUTPUT_DATA_FILE: lambda12.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    val lambda = { s1: String, s2: String ->
        println(s1)
        println(s2)
    }

    lambda("one", "two")

    return "OK"
}
