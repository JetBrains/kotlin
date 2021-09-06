/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lateinit.notInitialized

import kotlin.native.internal.*
import kotlin.test.*

class A {
    lateinit var s: String

    fun foo() = s
}

@Test fun runTest() {
    val frame = runtimeGetCurrentFrame()
    val a = A()
    try {
        println(a.foo())
    }
    catch (e: RuntimeException) {
        assertEquals(frame, runtimeGetCurrentFrame())
        println("OK")
        return
    }
    println("Fail")
}