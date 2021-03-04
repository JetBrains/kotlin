/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.branching.if_else

import kotlin.test.*

fun if_else(b: Boolean): Int {
  if (b) return 42
  else   return 24
}

@Test fun runTest() {
  if (if_else(false) != 24) throw Error()
}