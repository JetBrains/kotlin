/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.defaultArgs

import kotlin.test.*

class Z

inline fun Z.foo(x: Int = 42, y: Int = x) {
    println(y)
}

@Test fun runTest() {
    val z = Z()
    z.foo()
}