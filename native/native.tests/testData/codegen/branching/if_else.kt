/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun if_else(b: Boolean): Int {
  if (b) return 42
  else   return 24
}

fun box(): String {
  val res = if_else(false)
  if (res != 24) return "FAIL: $res"

  return "OK"
}