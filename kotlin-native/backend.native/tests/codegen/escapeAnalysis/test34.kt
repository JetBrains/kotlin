/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test34

open class C

object C0 : C()

class F(val k: Int) : C() {
    var f: C = if (k == 0) C0 else F(k - 1)
}

fun foo(k: Int, a: F): C {
    return if (k == 0) a else foo(k - 1, a.f as F)
}

fun bar(): F {
    val a = F(3)
    val v = foo(2, a) as F
    v.f = F(0)
    return ((a.f as F).f as F).f as F
}

fun main() = println(bar().k)