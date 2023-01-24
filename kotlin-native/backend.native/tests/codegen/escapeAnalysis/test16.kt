/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class A(val x: Int)

class F {
    var a = A(42)
}

class B {
    var f = F()
}

fun foo(b: B, f: F) {
    b.f.a = f.a
}

fun bar(x: Boolean): A {
    val b1 = B()
    val b2 = if (x) b1 else B()
    val f1 = F()
    f1.a = A(117)
    val f2 = if (!x) f1 else F().also { it.a = A(123) }
    foo(b2, f2)
    return b2.f.a
}

fun main() = println(bar(true).x)