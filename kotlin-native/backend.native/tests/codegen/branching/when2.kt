/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.branching.when2

import kotlin.test.*

fun when2(i: Int): Int {
  when (i) {
    0 -> return 42
    else -> return 24
  }
}

@Test fun runTest() {
  if (when2(0) != 42) throw Error()
}