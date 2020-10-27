/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda12

import kotlin.test.*

@Test fun runTest() {
    val lambda = { s1: String, s2: String ->
        println(s1)
        println(s2)
    }

    lambda("one", "two")
}