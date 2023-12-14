/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun cycle_for(arr: Array<Int>) : Int {
  var sum = 0
  for (i in arr) {
    sum += i
  }
  return sum
}

fun box(): String {
  assertEquals(6, cycle_for(Array(4, init = { it })))

  return "OK"
}