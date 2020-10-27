/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lateinit.localCapturedNotInitialized

import kotlin.test.*

@Test fun runTest() {
    lateinit var s: String

    fun foo() = s

    try {
        println(foo())
    }
    catch (e: RuntimeException) {
        println("OK")
        return
    }
    println("Fail")
}