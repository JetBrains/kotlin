/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun advanced_when5(i: Int): Int {
  when (i) {
    0 -> { val v = 42; return v}
    1 -> { val v = 42; return v}
    2 -> { val v = 42; return v}
    3 -> { val v = 42; return v}
    4 -> { val v = 42; return v}
    else -> return 24
  }
}

fun box(): String {
  val res = advanced_when5(5)
  if (res != 24) return "FAIL $res"

  return "OK"
}