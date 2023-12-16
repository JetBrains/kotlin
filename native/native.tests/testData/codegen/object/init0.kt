/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A(a:Int) {
  var i:Int = 0
  init {
    if (a == 0) i = 1
  }
}

fun box(): String {
   assertEquals(1, A(0).i)
   assertEquals(0, A(1).i)

   return "OK"
}