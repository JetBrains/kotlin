/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.for_loops_empty_range

import kotlin.test.*

@Test fun runTest() {
    // Simple loops
    for (i in 4..0) { print(i) }
    for (i in 4 until 0) { print(i) }
    for (i in 0 downTo 4) { print(i) }
    // Steps
    for (i in 4..0 step 2) { print(i) }
    for (i in 4 until 0 step 2) { print(i) }
    for (i in 0 downTo 4 step 2) { print(i) }
    // Two steps
    for (i in 6..0 step 2 step 3) { print(i) }
    for (i in 6 until 0 step 2 step 3) { print(i) }
    for (i in 0 downTo 6 step 2 step 3) { print(i) }

    println("OK")
}