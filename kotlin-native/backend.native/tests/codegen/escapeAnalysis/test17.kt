/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class A {
    var s = "zzz"
}

class B {
    var a = A()
}

fun foo(b1: B, b2: B): A {
    val a = A()
    a.s = "qxx"
    b1.a = a
    b2.a = a
    return a
}

fun bar(f: Boolean): String {
    val b1 = B()
    val b2 = if (f) b1 else B().also { it.a.s = "qzz" }
    val b3 = B()
    val a = foo(b2, b3)
    return a.s
}

fun main() = println(bar(true))