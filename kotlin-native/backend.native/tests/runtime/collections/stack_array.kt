/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.stack_array

import kotlin.test.*

@Test fun runTest() {
    val array = IntArray(2)
    array[0] = 1
    array[1] = 2
    val check = array is IntArray
    println(check)
    println(array[0] + array[1])
}