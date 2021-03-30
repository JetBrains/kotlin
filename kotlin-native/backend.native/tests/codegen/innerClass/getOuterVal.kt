/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.innerClass.getOuterVal

import kotlin.test.*

class Outer(val s: String) {
    inner class Inner {
        fun box() = s
    }
}

fun box() = Outer("OK").Inner().box()

@Test fun runTest()
{
    println(box())
}