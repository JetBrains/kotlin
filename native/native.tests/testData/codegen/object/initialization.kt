/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class A(val a:Int, val b:Int)

open class B(val c:Int, d:Int):A(c, d)

open class C(i:Int, j:Int):B(i + j, 42)

class D (i: Int, j:Int) : C(i, j){
   constructor(i: Int, j:Int, k:Int) : this(i, j) {
      foo(i)
   }
   constructor():this(1, 2)
}

fun foo(i:Int) : Unit {}


fun foo(i:Int, j:Int):Int {
   val c = D(i, j)
   return c.c
}

fun box(): String {
   assertEquals(5, foo(2, 3))
   return "OK"
}