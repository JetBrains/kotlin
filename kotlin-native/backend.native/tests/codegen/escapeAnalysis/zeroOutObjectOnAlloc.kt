/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.zeroOutObjectOnAlloc

import kotlin.test.*

class A {
    var x = 0
}

@Test fun runTest() {
    var sum1 = 0
    var sum2 = 0
    for (i in 0 until 10) {
        val a = A()
        sum1 += a.x
        a.x = i
        sum2 += a.x
    }
    assertEquals(0, sum1)
    assertEquals(45, sum2)
}