/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.initializers3

import kotlin.test.*

class Foo(val bar: Int)

var x = Foo(42)

@Test fun runTest() {
    println(x.bar)
}