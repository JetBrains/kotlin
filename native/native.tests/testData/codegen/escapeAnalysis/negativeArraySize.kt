/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.negativeArraySize

import kotlin.test.*

const val size = -42

fun foo() {
    val arr = IntArray(size) { it }
    for (x in arr) println(x)
}

@Test fun runTest() {
    assertFailsWith<IllegalArgumentException> { foo() }
}
