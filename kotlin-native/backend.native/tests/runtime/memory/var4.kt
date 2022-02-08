/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.var4

import kotlin.test.*

@Test fun runTest() {
    var x = Error()

    for (i in 0..1) {
        val c = Error()
        if (i == 0) x = c
    }

    // x refcount is 1.

    try {
        try {
            throw x
        } finally {
            x = Error()
        }
    } catch (e: Error) {
        e.use()
    }
}

fun Any?.use() {
    var x = this
}