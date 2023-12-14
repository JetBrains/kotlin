/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class TestClass {
    val x: Int

    init {
        x = 42
    }

    val y = x
}

fun box(): String {
    assertEquals(42, TestClass().y)
    return "OK"
}