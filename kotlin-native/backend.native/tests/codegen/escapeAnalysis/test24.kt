/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test24

open class C

object C0 : C()

class F(val k: Int) : C() {
    var f: C = if (k == 0) C0 else F(k - 1)
}

fun foo(): F {
    val a = F(3)
    var v: F = a
    var i = 0
    while (i != 0) {
        v = v.f as F
        i = i - 1
    }
    a.f = F(0)
    return v.f as F
}

fun main() = println(foo().k)