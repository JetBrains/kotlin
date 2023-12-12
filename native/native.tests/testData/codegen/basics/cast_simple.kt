/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class A() {}
class B(): A() {}

fun castSimple(o: Any) : A = o as A

fun castTest(): Boolean {
  val b = B()
  castSimple(b)
  return true
}

fun box(): String {
  if (!castTest()) throw Error()

  return "OK"
}