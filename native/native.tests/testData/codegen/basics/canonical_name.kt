/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

interface I<U, T> {
  fun foo(a: U): T
  fun qux(a: T): U
}

//-----------------------------------------------------------------------------//

class A1
class A2

//-----------------------------------------------------------------------------//

class A : I<A1, A2> {
  override fun foo(a: A1): A2 { sb.appendLine("A:foo"); return A2() }
  override fun qux(a: A2): A1 { sb.appendLine("A:qux"); return A1() }
}

//-----------------------------------------------------------------------------//

fun <U, V> baz(i: I<U, V>, u: U, v:V) {
  i.foo(u)
  i.qux(v)
}

//-----------------------------------------------------------------------------//

fun box(): String {
  baz<A1, A2>(A(), A1(), A2())

  assertEquals("A:foo\nA:qux\n", sb.toString())
  return "OK"
}