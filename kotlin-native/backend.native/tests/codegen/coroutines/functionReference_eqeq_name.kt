/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.coroutines.functionReference_eqeq_name

import kotlin.test.*

suspend fun foo(x: Int) = x

class Foo(val x: Int) {
    suspend fun bar() = x
}

@Test fun runTest() {
    val ref1 = ::foo
    val rec = Foo(42)
    val ref2 = rec::bar
    val ref3 = ::foo
    val ref4 = Foo(42)::bar
    val ref5 = rec::bar
    val ref6 = Foo::bar
    assertEquals("foo", ref1.name)
    assertEquals("bar", ref2.name)
    assertEquals("bar", ref6.name)
    assertFalse(ref1 == ref2)
    assertTrue(ref1 == ref3)
    assertFalse(ref2 == ref4)
    assertTrue(ref2 == ref5)
    assertFalse(ref6 == ref2)
}
