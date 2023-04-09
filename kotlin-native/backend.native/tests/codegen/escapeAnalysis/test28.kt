/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test28

class A {
    var s: String = "zzz"
}

class B {
    var a: A = A()
}

class MyException : Throwable()

fun foo(a: A, f: Boolean): B {
    val b = B()
    b.a = A()
    try {
        if (f)
            throw MyException()
        b.a = A()
    } catch (t: MyException) {
        b.a = a
        throw t
    }
    return b
}

fun main() = println(foo(A(), false))