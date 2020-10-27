/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.`try`.try3

import kotlin.test.*

@Test fun runTest() {
    val x = try {
        throw Error()
    } catch (e: Throwable) {
        6
    }

    println(x)
}