/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.tostring2

import kotlin.test.*

@Test fun runTest() {
    val hello = "Hello"
    val array = hello.toCharArray()
    for (ch in array) {
        print(ch)
        print(" ")
    }
    println()
    println(array.concatToString(0, array.size))
}