/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A(val a:Int) {
  fun foo(i:Int) = a + i
}

fun fortyTwo() = A(41).foo(1)

fun box(): String {
  assertEquals(42, fortyTwo())
  return "OK"
}