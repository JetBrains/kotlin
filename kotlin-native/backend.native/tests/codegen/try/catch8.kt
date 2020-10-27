/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.catch8

import kotlin.test.*

@Test fun runTest() {
    try {
        throw Error("Error happens")
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            println(message)
        }
    }
}