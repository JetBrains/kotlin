/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.finally11

import kotlin.test.*

@Test fun runTest() {
    try {
        try {
            return
        } catch (e: Error) {
            println("Catch 1")
        } finally {
            println("Finally")
            throw Error()
        }
    } catch (e: Error) {
        println("Catch 2")
    }

    println("Done")
}