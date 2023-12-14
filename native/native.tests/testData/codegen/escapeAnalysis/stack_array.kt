/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

fun box(): String {
    val array = IntArray(2)
    array[0] = 1
    array[1] = 2
    val check = array is IntArray
    assertTrue(check)
    assertEquals(3, array[0] + array[1])

    return "OK"
}