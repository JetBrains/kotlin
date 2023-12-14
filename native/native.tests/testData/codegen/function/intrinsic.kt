/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// This code fails to link when bultins are taken from the 
// frontend generated module, instead of our library.
// Because the generated module doesn't see our name changing annotations,
// the function names are all wrong.

fun intrinsic(b: Int): Int {
  var sum = 1
  sum = sum + b
  return sum
}

fun box(): String {
  assertEquals(4, intrinsic(3))
  return "OK"
}