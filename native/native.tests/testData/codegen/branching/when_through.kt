/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun when_through(i: Int): Int {
  var value = 1
  when (i) {
    10 -> value = 42
    11 -> value = 43
    12 -> value = 44
  }

  return value
}

fun box(): String {
  val res = when_through(2)
  if (res != 1) return "FAIL $res"

  return "OK"
}