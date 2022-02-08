/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.initializers.correctOrder1

import kotlin.test.*

class TestClass {
    val x: Int

    init {
        x = 42
    }

    val y = x
}

@Test fun runTest() {
    println(TestClass().y)
}