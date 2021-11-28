/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.inline24

import kotlin.test.*

fun foo() = println("foo")
fun bar() = println("bar")

inline fun baz(x: Unit = foo(), y: Unit) {}

@Test fun runTest() {
    baz(y = bar())
}
