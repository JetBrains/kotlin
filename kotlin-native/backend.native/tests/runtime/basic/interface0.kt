/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.interface0

import kotlin.test.*

interface A {
    fun b() = c()
    fun c()
}

class B(): A {
    override fun c() {
        println("PASSED")
    }
}

@Test fun runTest() {
    val a:A = B()
    a.b()
}

