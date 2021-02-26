/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.localClass.tryCatch

import kotlin.test.*

private fun foo() {
    val local =
            object {
                fun bar() {
                    try {
                    } catch (t: Throwable) {
                        println(t)
                    }
                }
            }
    local.bar()
}

@Test fun runTest() {
    foo()
}