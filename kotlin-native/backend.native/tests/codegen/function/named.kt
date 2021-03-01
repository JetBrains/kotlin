/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.function.named

import kotlin.test.*

fun foo(a:Int, b:Int) = a - b

@Test fun runTest() {
  if (foo(b = 24, a = 42) != 18)
      throw Error()
}