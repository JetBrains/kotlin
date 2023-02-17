/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

open class C

object C0 : C()

class F(val k: Int) : C() {
    var f: C = C0
}

fun foo(): F {
    val a = F(3)
    var v: F = a
    var i = 2
    do {
        val f = F(i)
        v.f = f
        v = f
        i = i - 1
    } while (i > 0)
    v.f = F(0)
    return ((a.f as F).f as F).f as F
}

fun main() = println(foo().k)