/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun plus_eq(a: Int): Int {
  var b = 11
  b += a
  return b
}

fun box(): String {
  assertEquals(14, plus_eq(3))
  return "OK"
}