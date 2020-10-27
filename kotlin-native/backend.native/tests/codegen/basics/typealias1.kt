/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.typealias1

import kotlin.test.*

@Test
fun runTest() {
    println(Bar(42).x)
}

class Foo(val x: Int)
typealias Bar = Foo