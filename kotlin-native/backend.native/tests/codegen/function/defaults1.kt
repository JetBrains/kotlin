/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.defaults1

import kotlin.test.*

fun foo(x:Int = 0, y:Int = x + 1, z:Int = x + y + 1) = x + y + z

@Test fun runTest() {
  val v = foo()
  if (v != 3) {
    println("test failed $v expected 3")
    throw  Error()
  }
}