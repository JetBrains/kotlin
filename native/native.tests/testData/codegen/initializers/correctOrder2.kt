// OUTPUT_DATA_FILE: correctOrder2.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class TestClass {
    val x: Int

    val y = 42

    init {
        x = y
    }
}

fun box(): String {
    println(TestClass().x)

    return "OK"
}
