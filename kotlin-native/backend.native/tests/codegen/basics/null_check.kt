/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.null_check

import kotlin.test.*

//--- Test "eqeq" -------------------------------------------------------------//

fun check_eqeq(a: Any?) = a == null

fun null_check_eqeq1() : Boolean {
  return check_eqeq(Any())
}

fun null_check_eqeq2() : Boolean {
  return check_eqeq(null)
}

//--- Test "eqeqeq" -----------------------------------------------------------//

fun check_eqeqeq(a: Any?) = a === null

fun null_check_eqeqeq1() : Boolean {
  return check_eqeqeq(Any())
}

fun null_check_eqeqeq2() : Boolean {
  return check_eqeqeq(null)
}

@Test
fun runTest() {
  if (null_check_eqeq1())    throw Error()
  if (!null_check_eqeq2())   throw Error()
  if (null_check_eqeqeq1())  throw Error()
  if (!null_check_eqeqeq2()) throw Error()
}