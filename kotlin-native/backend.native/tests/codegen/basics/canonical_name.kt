/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.canonical_name

import kotlin.test.*

interface I<U, T> {
  fun foo(a: U): T
  fun qux(a: T): U
}

//-----------------------------------------------------------------------------//

class A1
class A2

//-----------------------------------------------------------------------------//

class A : I<A1, A2> {
  override fun foo(a: A1): A2 { println("A:foo"); return A2() }
  override fun qux(a: A2): A1 { println("A:qux"); return A1() }
}

//-----------------------------------------------------------------------------//

fun <U, V> baz(i: I<U, V>, u: U, v:V) {
  i.foo(u)
  i.qux(v)
}

//-----------------------------------------------------------------------------//

@Test
fun runTest() {
  baz<A1, A2>(A(), A1(), A2())
}