/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package lower.vararg_of_literals

import kotlin.test.*

@Test fun runTest() {
    foo()
    foo()
}

fun foo() {
    val array = arrayOf("a", "b")
    println(array[0])
    array[0] = "42"
}