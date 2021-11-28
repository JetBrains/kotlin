/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults8

import kotlin.test.*

class Foo {
    fun test(x: Int = 1) = x
}

class Bar {
    fun test(x: Int = 2) = x
}

@Test fun runTest() {
    println(Bar().test())
}