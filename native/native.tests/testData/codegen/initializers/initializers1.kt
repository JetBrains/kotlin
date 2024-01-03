/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.initializers1

import kotlin.test.*

class TestClass {
    companion object {
        init {
            println("Init Test")
        }
    }
}

@Test fun runTest() {
    val t1 = TestClass()
    val t2 = TestClass()
    println("Done")
}