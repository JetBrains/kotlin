/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.innerClass.simple

import kotlin.test.*

class Outer {
    inner class Inner {
        fun box() = "OK"
    }
}

fun box() = Outer().Inner().box()

@Test fun runTest()
{
    println(box())
}