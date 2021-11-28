/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`object`.initialization1

import kotlin.test.*

class TestClass {
    constructor() {
        println("constructor1")
    }

    constructor(x: Int) : this() {
        println("constructor2")
    }

    init {
        println("init")
    }

    val f = println("field")
}

@Test fun runTest() {
    TestClass()
    TestClass(1)
}