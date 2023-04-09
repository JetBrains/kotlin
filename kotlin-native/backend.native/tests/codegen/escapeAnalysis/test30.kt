/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test30

class A {
    var s: String = "zzz"
}

class B {
    var a: A = A()
}

class MyException(val a: A) : Throwable()

fun foo(a: A, f: Boolean) {
    if (f) throw MyException(a)
}

fun bar(a: A, f: Boolean): A {
    val a2 = if (f) a else A()
    foo(a2, f)
    return a
}

fun main() = println(bar(A(), false))