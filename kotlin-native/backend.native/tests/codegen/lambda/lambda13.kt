/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lambda.lambda13

import kotlin.test.*

@Test fun runTest() {
    apply("foo") {
        println(this)
    }
}

fun apply(str: String, block: String.() -> Unit) {
    str.block()
}