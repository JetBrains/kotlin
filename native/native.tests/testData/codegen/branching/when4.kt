/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun when5(i: Int): Int {
  when (i) {
    0 -> return 42
    1 -> return 4
    2 -> return 3
    3 -> return 2
    4 -> return 1
  }
  return 24
}

fun box(): String {
  if (when5(0) != 42) return "FAIL 0"
  if (when5(1) != 4) return "FAIL 1"
  if (when5(2) != 3) return "FAIL 2"
  if (when5(3) != 2) return "FAIL 3"
  if (when5(4) != 1) return "FAIL 4"
  if (when5(5) != 24) return "FAIL 5"

  return "OK"
}
