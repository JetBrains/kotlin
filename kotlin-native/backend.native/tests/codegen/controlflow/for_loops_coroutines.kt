/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.for_loops_coroutines

import kotlin.test.*

import kotlin.coroutines.*

@Test fun runTest() {
    val sq = sequence {
        for (i in 0..6 step 2) {
            print("before: $i ")
            yield(i)
            println("after: $i")
        }
    }
    println("Got: ${sq.joinToString(separator = " ")}")
}