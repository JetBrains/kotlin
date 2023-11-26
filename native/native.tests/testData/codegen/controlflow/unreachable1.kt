// OUTPUT_DATA_FILE: unreachable1.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    println(foo())

    return "OK"
}

fun foo(): Int {
    return 1
    println("After return")
}
