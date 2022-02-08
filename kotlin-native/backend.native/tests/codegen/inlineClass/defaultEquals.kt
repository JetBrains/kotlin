/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inlineClass.defaultEquals

import kotlin.test.*

inline class A(val x: Int)
inline class B(val a: A)
inline class C(val s: String)
inline class D(val c: C)

@Test fun runTest() {
    val a = A(42)
    val b = B(a)
    val c = C("zzz")
    val d = D(c)
    assertTrue(a.equals(a))
    assertTrue(b.equals(b))
    assertTrue(c.equals(c))
    assertTrue(d.equals(d))
}
