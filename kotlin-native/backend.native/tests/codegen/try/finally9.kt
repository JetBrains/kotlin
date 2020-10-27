/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.finally9

import kotlin.test.*

@Test fun runTest() {
    do {
        try {
            break
        } finally {
            println("Finally 1")
        }
    } while (false)

    var stop = false
    while (!stop) {
        try {
            stop = true
            continue
        } finally {
            println("Finally 2")
        }
    }

    println("After")
}