/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
  val list0 = _arrayOf("K", "o", "t", "l", "i", "n")
  val list1 = _arrayOf("l", "a","n", "g", "u", "a", "g", "e")
  val list = foo(list0, list1)

  val expected = listOf("K", "o", "t", "l", "i", "n", " ", "i", "s", " ", "c", "o", "o", "l", " ", "l", "a", "n", "g", "u", "a", "g", "e")
  if (list != expected)
    return "FAIL: $list"
  return "OK"
}


fun foo(a:Array<out String>, b:Array<out String>) = listOf(*a," ", "i", "s", " ", "c", "o", "o", "l", " ", *b)


fun _arrayOf(vararg arg:String) = arg