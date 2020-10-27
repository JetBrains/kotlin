/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.exceptions.catch7

import kotlin.test.*

@Test fun runTest() {
    try {
        foo()
    } catch (e: Throwable) {
        val message = e.message
        if (message != null) {
            println(message)
        }
    }
}

fun foo() {
    throw Error("Error happens")
}