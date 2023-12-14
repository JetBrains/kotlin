/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun sum(a:Int, b:Int):Int {
 var c:Int
 c = a + b
 return c
}

fun box(): String {
  assertEquals(50, sum(42, 8))
  return "OK"
}
