/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.lateinit.localCapturedInitialized

import kotlin.test.*

@Test fun runTest() {
    lateinit var s: String

    fun foo() = s

    s = "zzz"
    println(foo())
}