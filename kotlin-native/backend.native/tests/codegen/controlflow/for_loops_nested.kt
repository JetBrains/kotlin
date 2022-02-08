/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.for_loops_nested

import kotlin.test.*

@Test fun runTest() {
    // Simple
    for (i in 0..2) {
        for (j in 0..2) {
            print("$i$j ")
        }
    }
    println()

    // Break
    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            print("$i$j ")
            if (j == 1) break
        }
    }
    println()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            print("$i$j ")
            if (j == 1) break@l2
        }
    }
    println()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            print("$i$j ")
            if (j == 1) break@l1
        }
    }
    println()

    // Continue
    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            if (j == 1) continue
            print("$i$j ")
        }
    }
    println()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            if (j == 1) continue@l2
            print("$i$j ")
        }
    }
    println()

    l1@for (i in 0..2) {
        l2@for (j in 0..2) {
            if (j == 1) continue@l1
            print("$i$j ")
        }
    }
    println()
}