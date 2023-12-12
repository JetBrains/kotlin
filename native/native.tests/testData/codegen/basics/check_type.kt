/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

interface I
class A() : I {}
class B() {}

//-----------------------------------------------------------------------------//

fun isTypeOf(a: Any?) : Boolean {
  return a is A
}

//-----------------------------------------------------------------------------//

fun isTypeNullableOf(a: Any?) : Boolean {
  return a is A?
}

//-----------------------------------------------------------------------------//

fun isNotTypeOf(a: Any) : Boolean {
  return a !is A
}

//-----------------------------------------------------------------------------//

fun isTypeOfInterface(a: Any) : Boolean {
  return a is I
}

//-----------------------------------------------------------------------------//

fun box(): String {
  if (!isTypeOf(A())) return "FAIL !isTypeOf(A())"
  if (isTypeOf(null)) return "FAIL isTypeOf(null)"
  if (!isTypeNullableOf(A())) return "FAIL !isTypeNullableOf(A())"
  if (!isTypeNullableOf(null)) return "FAIL !isTypeNullableOf(null)"
  if (!isNotTypeOf(B())) return "FAIL !isNotTypeOf(B())"
  if (!isTypeOfInterface(A())) return "FAIL !isTypeOfInterface(A())"

  return "OK"
}
