/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.for_loops

import kotlin.test.*

@Test fun runTest() {

    // Simple loops
    for (i in 0..4) {
        print(i)
    }
    println()

    for (i in 0 until 4) {
        print(i)
    }
    println()

    for (i in 4 downTo 0) {
        print(i)
    }
    println()
    println()

    // Steps
    for (i in 0..4 step 2) {
        print(i)
    }
    println()

    for (i in 0 until 4 step 2) {
        print(i)
    }
    println()

    for (i in 4 downTo 0 step 2) {
        print(i)
    }
    println()
    println()


    // Two steps
    for (i in 0..6 step 2 step 3) {
        print(i)
    }
    println()

    for (i in 0 until 6 step 2 step 3) {
        print(i)
    }
    println()

    for (i in 6 downTo 0 step 2 step 3) {
        print(i)
    }
    println()
    println()

    // Without constants
    val a = 0
    val b = 4
    val s = 2
    for (i in a..b step s) {
        print(i)
    }
    println()

    for (i in a until b step s) {
        print(i)
    }
    println()

    for (i in b downTo a step s) {
        print(i)
    }
    println()
    println()
}