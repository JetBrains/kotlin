/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test31

class A {
    var s: String = "zzz"
}

class B {
    var a: A = A()
}

fun foo(b: B, a: A) {
    b.a = a
    throw IllegalStateException()
}

fun bar(): B {
    val a = A()
    val b = B()
    try {
        foo(b, a)
    } catch (t: Throwable) { }
    return b
}

fun main() = println(bar())