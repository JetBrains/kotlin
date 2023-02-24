/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

open class C

object C0 : C()

class F(val k: Int) : C() {
    var f: C = if (k == 0) C0 else F(k - 1)
}

fun foo(a: F): F {
    var v: F = a
    var i = 0
    do {
        v = v.f as F
        i = i + 1
    } while (i < 2)
    return v
}

fun bar(): F {
    val a = F(3)
    a.f = F(2)
    (a.f as F).f = F(1)
    val v = foo(a)
    return v
}

fun main() = println(bar().k)