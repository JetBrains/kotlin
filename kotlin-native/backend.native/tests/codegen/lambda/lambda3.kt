/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda3

import kotlin.test.*

@Test fun runTest() {
    var str = "lambda"
    run {
        println(str)
    }
}

fun run(f: () -> Unit) {
    f()
}