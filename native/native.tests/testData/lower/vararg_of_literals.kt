// OUTPUT_DATA_FILE: vararg_of_literals.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    foo()
    foo()

    return "OK"
}

fun foo() {
    val array = arrayOf("a", "b")
    println(array[0])
    array[0] = "42"
}