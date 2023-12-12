/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun local_variable(a: Int) : Int {
  var b = 0
  b = a + 11
  return b
}

fun box(): String {
  if (local_variable(3) != 14) throw Error()
  return "OK"
}