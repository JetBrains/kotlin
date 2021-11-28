/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.localObjectReturnedFromWhen

import kotlin.test.*

fun foo() {
    123?.let {
        object : () -> Unit {
            override fun invoke() = Unit
        }
    }
}

@Test fun runTest() {
    foo()
    println("Ok")
}