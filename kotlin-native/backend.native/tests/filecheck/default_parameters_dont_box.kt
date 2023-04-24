/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

value class A(val i: Int)
value class B(val a: A)
value class C(val s: String)

fun defaultInt(a: Int = 1, aa: Int = 1) = a
fun defaultA(a: A = A(1), aa: A = A(1)) = a.i
fun defaultB(b: B = B(A(1)), bb: B = B(A(1))) = b.a.i
fun defaultC(c: C = C("1"), cc: C = C("1")) = c.s

// CHECK-LABEL: "kfun:#main(){}"
fun main(){
    // CHECK-LABEL: entry
    // CHECK-NOT: <Int-box>
    // CHECK-NOT: <A-box>
    // CHECK-NOT: <B-box>
    // CHECK-NOT: <C-box>

    defaultInt()
    defaultA()
    defaultB()
    defaultC()
    defaultInt(1)
    defaultA(A(1))
    defaultB(B(A(1)))
    defaultC(C("1"))
    defaultInt(1, 1)
    defaultA(A(1), A(1))
    defaultB(B(A(1)), B(A(1)))
    defaultC(C("1"), C("1"))
    // CHECK-LABEL: epilogue
}