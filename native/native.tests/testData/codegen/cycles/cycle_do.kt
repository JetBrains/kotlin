/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun cycle_do(cnt: Int): Int {
  var sum = 1
  do {
    sum = sum + 2
  } while (sum == cnt)
  return sum
}

fun box(): String {
  assertEquals(5, cycle_do(3))
  assertEquals(3, cycle_do(0))

  return "OK"
}