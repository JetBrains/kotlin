/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test25

open class C

object C0 : C()

class F(val k: Int) : C() {
    var f: C = if (k == 0) C0 else F(k - 1)
}

fun foo(k: Int): F {
    val a = F(3)
    val v = when (k) {
        1 -> a.f as F
        2 -> (a.f as F).f as F
        else -> a
    }
    v.f = F(0)
    return ((a.f as F).f as F).f as F
}

fun main() = println(foo(2).k)